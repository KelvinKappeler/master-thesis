# PrintWizard

PrintWizard is a research prototype for exploring Java program execution through interactive traces and object inspection. This was my **Master's project in Computer Science at EPFL (2025)**.

## Run the demo

Install Node.js (with npm), then run from the repository root:

```sh
cd Frontend
npm install
npm start
```

Open http://localhost:8000. The demo loads the bundled trace in `Backend/Results/New` automatically. No Java compilation is required. Some entries in the Files menu use an older data format and are not compatible with the current viewer.

## Repository

- **Frontend/**: browser-based trace viewer and local Node.js server.
- **Backend/**: Java instrumentation, parsing, example programs, and recorded traces. The Java sources target JDK 21 and are organized as Maven modules.
