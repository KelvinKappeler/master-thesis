import {JsonData} from "./JsonData.js";
import {Preconditions} from "../utils/Preconditions.js";
import {MainDataAssembler} from "./MainDataAssembler.js";
import {TraceModel} from "../model/TraceModel.js";
import {TraceContainer} from "../view/TraceContainer.js";
import {TraceView} from "../view/TraceView.js";
import {ObjectInspector} from "../elements/ObjectInspector.js";
import {SpanInspector} from "../elements/SpanInspector.js";

/**
 * This class is responsible to manage PrintWizard
 */
export class PrintWizard {
    constructor() {
        this.jsonData = undefined;
        this.objectInspector = null;

        /*this.objectInspector = new ObjectInspector();
        this.objectInspector.attachTo(document.querySelector('#inspector'));
        this.searchInspector = new SearchInspector();
        this.searchInspector.attachTo(document.querySelector('#inspector'));*/
    }

    /**
     * This method is used to load data from the given location
     * @param manifestLocation {string} The location of the manifest.json file
     */
    loadData(manifestLocation) {
        Preconditions.requireNonNull(manifestLocation, "manifestLocation is null");

        console.clear();
        this.jsonData = new JsonData(manifestLocation);

        MainDataAssembler.assemble(this.jsonData).then((mainData) => {
            const trace = new TraceModel(mainData);
            const traceContainer = new TraceContainer(
                document.querySelector('.traceContent'),
                document.querySelector('.lineNumbers'),
                document.querySelector('.traceTriangles')
            );

            const traceView = new TraceView(trace, traceContainer);

            this.objectInspector = new ObjectInspector(trace);
            this.objectInspector.attachTo(document.querySelector('#inspector'));
            this.openInspectorTab("objectInspector");

            this.spanInspector = new SpanInspector(trace);
            this.spanInspector.attachTo(document.querySelector('#inspector'));

            window.addEventListener("pw:inspect-object", (e) => {
                const objectId = e?.detail?.objectId;
                const eventId = e?.detail?.eventId;

                this.objectInspector.add(objectId, eventId);
            });
            window.addEventListener("pw:inspect-span", (e) => {
                const spanId = e?.detail?.spanId;
                if (!spanId) return;
                this.openInspectorTab("spanInspector");
                this.spanInspector.select(spanId);
            });
            window.addEventListener("pw:inspect-span-from-event", (e) => {
                const eventId = e?.detail?.eventId;
                if (!eventId) return;
                this.openInspectorTab("spanInspector");
                this.spanInspector.selectFromEvent(eventId);
            });
            window.addEventListener("pw:reveal-event", (e) => {
                const eventId = e?.detail?.eventId;
                traceView.revealEvent(eventId);
            });
            window.addEventListener("pw:preview-event", (e) => {
                const eventId = e?.detail?.eventId;
                const on = !!e?.detail?.on;
                traceView.previewEvent(eventId, on);
            });


            traceView.render();
        });

        this.#initSplitter();
    }

    openInspectorTab(inspectorName) {
        let tabs = document.getElementsByClassName("inspectorContent");
        for (let i = 0; i < tabs.length; i++) {
            tabs[i].style.display = "none";
        }
        document.getElementById(inspectorName).style.display = "block";

        let buttons = document.getElementsByClassName("tab_button");
        for (let i = 0; i < buttons.length; i++) {
            buttons[i].classList.remove("active");
        }

        let activeButton = document.querySelector(`button[onclick="pw.openInspectorTab('${inspectorName}')"]`);
        activeButton.classList.add("active");
    }

    #initSplitter() {
        const main = document.querySelector("main");
        const trace = document.querySelector(".trace");
        const inspector = document.querySelector("#inspector");
        const splitter = document.querySelector("#splitter");

        if (!main || !trace || !inspector || !splitter) return;

        let dragging = false;

        const onMouseMove = (e) => {
            if (!dragging) return;

            const rect = main.getBoundingClientRect();

            let newTraceWidth = e.clientX - rect.left;

            trace.style.flex = `0 0 ${newTraceWidth}px`;
            inspector.style.flex = `1 1 auto`;
        };

        const stopDragging = () => {
            if (!dragging) return;
            dragging = false;
            main.classList.remove("resizing");
            window.removeEventListener("mousemove", onMouseMove);
            window.removeEventListener("mouseup", stopDragging);
        };

        splitter.addEventListener("mousedown", (e) => {
            e.preventDefault();
            dragging = true;
            main.classList.add("resizing");
            window.addEventListener("mousemove", onMouseMove);
            window.addEventListener("mouseup", stopDragging);
        });
    }

}
