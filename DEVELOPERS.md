# Build

```bash
mvn clean install
```

This by default skips Failsafe plugin (for integration tests).

# Run integration tests

First create `.env.local` file with the following content:

```ini
OPENAI_API_KEY=your-openai-api-key-or-empty-if-you-have-none
ANTHROPIC_API_KEY=your-anthropic-api-key-or-empty-if-you-have-none
GOOGLE_API_KEY=your-google-api-key-or-empty-if-you-have-none
XAI_API_KEY=your-xai-api-key-or-empty-if-you-have-none
META_API_KEY=your-meta-api-key-or-empty-if-you-have-none
AZURE_API_KEY=your-azure-api-key-or-empty-if-you-have-none
OPENROUTER_API_KEY=your-openrouter-api-key-or-empty-if-you-have-none
OLLAMA_API_KEY=no-key-needed-but-non-empty-env-var-will-trigger-the-test-if-you-have-ollama-installed
```

Then ensure that `it.sh` is executable:

```bash
chmod +x it.sh
```

Then run it:

```
./it.sh
```

This by default skips Javadoc plugin.
