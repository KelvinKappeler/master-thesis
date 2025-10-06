import {Preconditions} from "../utils/Preconditions.js";

/**
 * Represents the entire json data structure.
 * @param {string} manifestLocation - Location of the manifest file.
 */
export class JsonData {
    constructor(manifestLocation) {
        Preconditions.requireNonNull(manifestLocation);

        const basePath = manifestLocation.substring(0, manifestLocation.lastIndexOf('/') + 1);

        this.ready = this.loadAll(manifestLocation, basePath);
    }

    async loadAll(manifestLocation, basePath) {
        const manifestResponse = await fetch(manifestLocation);
        if (!manifestResponse.ok) throw new Error("Failed to load manifest");
        this.manifestFile = await manifestResponse.json();

        const files = this.manifestFile.fileLocations;
        const programPath = basePath + files.programFilePath;
        const tracePath = basePath + files.traceFilePath;
        const indexPath = basePath + files.indexFilePath;

        const [programRes, traceRes, indexRes] = await Promise.all([
            fetch(programPath),
            fetch(tracePath),
            fetch(indexPath)
        ]);

        if (!programRes.ok || !traceRes.ok || !indexRes.ok) {
            throw new Error("Failed to load one or more data files");
        }

        this.programFile = await programRes.json();
        this.traceFile = await traceRes.json();
        this.indexFile = await indexRes.json();

        return this;
    }

    /**
     * Returns all data from the JSON files.
     * @returns {Promise<[programFile, traceFile, indexFile]>}
     * A promise that resolves to an array containing the program, trace, and index data.
     */
    async getAllData() {
        await this.ready;

        return [this.programFile, this.traceFile, this.indexFile];
    }
}
