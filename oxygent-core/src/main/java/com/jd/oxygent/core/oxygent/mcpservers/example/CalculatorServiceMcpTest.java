package com.jd.oxygent.core.oxygent.mcpservers.example;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Test client for MCP (Model Context Protocol) server using CalculatorService.
 * Demonstrates how to communicate with an MCP server via stdio, including initialization,
 * tool listing, and tool invocation.
 */
public class CalculatorServiceMcpTest {

	/**
	 * Main method to run the MCP client test.
	 * 
	 * @param args Command line arguments
	 * @throws Exception If any error occurs during test execution
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("=== Simple MCP Client Test ===");

		// Use relative path to simplify classpath
		String classpath = System.getProperty("java.class.path");

		ProcessBuilder pb = new ProcessBuilder("java", "-cp", classpath,
				"com.jd.oxygent.core.oxygent.mcpservers.example.CalculatorService");

		pb.redirectErrorStream(true);

		System.out.println("Starting server...");
		Process process = pb.start();

		// Thread to read server output
		Thread readerThread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println("SERVER: " + line);
				}
			}
			catch (IOException e) {
				System.out.println("Reader ended: " + e.getMessage());
			}
		});
		readerThread.setDaemon(true);
		readerThread.start();

		// Get write stream
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

		// Wait for server to start
		Thread.sleep(2000);

		// Send request sequence - Important: Send initialized notification after initialize
		String[] requests = {
				"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\"}}",
				// !!!! Required initialization completion notification !!!!
				"{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}",
				"{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}",
				// Fixed: Call calculator tool (using correct tool name and parameter structure)
				"{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"calculator\",\"arguments\":{\"operation\":\"add\",\"a\":10,\"b\":5}}}",
				// Added: Call weather tool
				"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"get_current_weather\",\"arguments\":{\"location\":\"Beijing\",\"format\":\"celsius\"}}}"
		};

		for (String request : requests) {
			System.out.println("\nCLIENT >>> " + request);
			writer.write(request + "\n");
			writer.flush();
			Thread.sleep(2000); // Wait for response
		}

		// Wait a bit to see responses
		Thread.sleep(5000);

		// Cleanup
		System.out.println("\nEnding test...");
		process.destroy();
		writer.close();

		if (process.waitFor(3, TimeUnit.SECONDS)) {
			System.out.println("Server exit code: " + process.exitValue());
		}
	}
}