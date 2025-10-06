import {SourceFormat} from "./SourceFormat.js";
import {ObjectData} from "./ObjectData.js";
import {Trace} from "./EventTrace.js";
import {Preconditions} from "../utils/Preconditions.js";

/**
 * This class contains the data from the json files of the backend.
 */
export class JsonData {
    static sourceFormatFile = "source_format.json";
    static objectDataFile = "objectData.json";
    static traceFile = "eventTrace.json";

    /**
     * Creates a new JsonData object.
     * @param location {string} The location of the json files.
     */
    constructor(location) {
        Preconditions.checkIfString(location);

        this.sourceFormat = fetch(location + JsonData.sourceFormatFile)
            .then(response => response.json())
            .then(data => { return new SourceFormat(data) });

        this.objectData = fetch(location + JsonData.objectDataFile)
            .then(response => response.json())
            .then(data => { return new ObjectData(data) });

        this.eventTrace = fetch(location + JsonData.traceFile)
            .then(response => response.json())
            .then(data => { return new Trace(data) });
    }

    /**
     * Returns all data from the json files.
     * @returns {Promise<[]>} A promise that resolves to an array containing the source format, object data and event trace.
     */
    getAllData() {
        return Promise.all([this.sourceFormat, this.objectData, this.eventTrace]);
    }
}
