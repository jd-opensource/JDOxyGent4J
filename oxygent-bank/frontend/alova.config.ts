export default {
  generator: [
    {
      input: './openapi/swagger.json',
      output: 'src/api',
      global: 'Apis',
    },
  ],
  autoUpdate: false,
}
