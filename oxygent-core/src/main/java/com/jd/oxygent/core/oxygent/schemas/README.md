# OxyGent Java Schemas

This directory contains Java versions of the Python schemas from the `oxygent/schemas` folder.

## Files Overview

- **Color.java** - Color enumeration for terminal colors
- **LLM.java** - LLM state and response models
- **Memory.java** - Message and memory management classes
- **Web.java** - Web response models
- **Observation.java** - Observation and execution result models
- **Oxy.java** - Core Oxy request/response and state management

## Dependencies

To use these Java classes, you'll need the following dependencies:

### Maven Dependencies

```xml
<dependencies>
    <!-- Jackson for JSON processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-core</artifactId>
        <version>2.15.2</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-annotations</artifactId>
        <version>2.15.2</version>
    </dependency>
</dependencies>
```

### Gradle Dependencies

```gradle
dependencies {
    implementation 'com.fasterxml.jackson.core:jackson-core:2.15.2'
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.2'
}
```

## Usage Examples

### Creating Messages

```java
import com.jd.oxygent.schemas.Memory;

// Create different types of messages
Memory.Message userMsg = Memory.Message.userMessage("Hello, how are you?");
Memory.Message systemMsg = Memory.Message.systemMessage("You are a helpful assistant.");
Memory.Message assistantMsg = Memory.Message.assistantMessage("I'm doing well, thank you!");

// Create memory buffer
Memory.MemoryBuffer memory = new Memory.MemoryBuffer(20);
memory.addMessage(systemMsg);
memory.addMessage(userMsg);
memory.addMessage(assistantMsg);
```

### Working with Oxy Requests

```java
import com.jd.oxygent.schemas.Oxy;
import java.util.HashMap;
import java.util.Map;

// Create an Oxy request
Oxy.OxyRequest request = new Oxy.OxyRequest();
request.setCallee("search_tool");

Map<String, Object> args = new HashMap<>();
args.put("query", "Java programming");
request.setArguments(args);

// Create response
Oxy.OxyResponse response = new Oxy.OxyResponse(
    Oxy.OxyState.COMPLETED, 
    "Search completed successfully"
);
```

### Web Responses

```java
import com.jd.oxygent.schemas.Web;
import java.util.HashMap;
import java.util.Map;

// Create a web response
Map<String, Object> data = new HashMap<>();
data.put("result", "Operation successful");
data.put("timestamp", System.currentTimeMillis());

Web.WebResponse response = new Web.WebResponse(200, "SUCCESS", data);
Map<String, Object> responseDict = response.toDict();
```

## Key Differences from Python Version

1. **Type Safety**: Java provides compile-time type checking
2. **Enums**: Java enums are more structured than Python enums
3. **Generics**: Used for type-safe collections
4. **Builder Pattern**: Can be implemented for complex object creation
5. **Immutability**: Consider using final fields and builder patterns for immutable objects
6. **Null Safety**: Proper null checking should be implemented

## Notes

- The `Union` types from Python are represented as `Object` in Java
- Some Python-specific features like `__add__` operators are implemented as regular methods
- Async operations use `CompletableFuture` instead of Python's `async/await`
- JSON serialization/deserialization should be handled with Jackson annotations

## Future Improvements

- Add validation annotations (JSR-303)
- Implement builder patterns for complex objects
- Add proper exception handling
- Consider using records (Java 14+) for simple data classes
- Add unit tests
- Implement proper deep copying mechanisms