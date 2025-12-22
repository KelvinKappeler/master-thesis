import {JsonData} from "./JsonData.js";
import {Preconditions} from "../utils/Preconditions.js";
import {MainDataAssembler} from "./MainDataAssembler.js";
import {TraceModel} from "../model/TraceModel.js";
import {TraceContainer} from "../view/TraceContainer.js";
import {TraceView} from "../view/TraceView.js";

/**
 * This class is responsible to manage PrintWizard
 */
export class PrintWizard {
    constructor() {
        this.jsonData = undefined;
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

            traceView.render();
        });

        this.#initSplitter();

        /*this.jsonData.getAllData().then(data => {
            const finalTreeTrace = translateToTreeFormat(data[2], data[0], data[1]).inlineLoops();
            console.log(finalTreeTrace);

            this.trace = new TraceModel(finalTreeTrace);
            this.trace.show();
            this.parser = new Parser(this.trace);

            this.breadcrumb.clear();
            this.breadcrumb.add(data[0].sourceFile.fileName);
            this.breadcrumb.add(finalTreeTrace.name + "()");

            this.openInspectorTab('objectInspector');
            this.objectInspector.clear();

            this.searchInspector.clearResult();
        });
    }

    openInspectorTab(inspectorName) {
        let tabs = document.getElementsByClassName("inspectorContent");
        for (let i = 0; i < tabs.length; i++) {
            tabs[i].style.display = "none";
        }
        document.getElementById(inspectorName).style.display = "block";

        // Remove active class from all buttons
        let buttons = document.getElementsByClassName("tab_button");
        for (let i = 0; i < buttons.length; i++) {
            buttons[i].classList.remove("active");
        }

        // Add active class to the clicked button
        let activeButton = document.querySelector(`button[onclick="pw.openInspectorTab('${inspectorName}')"]`);
        activeButton.classList.add("active");
    */}

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
