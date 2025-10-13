import {JsonData} from "./JsonData.js";
import {Preconditions} from "../utils/Preconditions.js";
import {MainDataAssembler} from "./MainDataAssembler.js";
import {TraceModel} from "../model/TraceModel.js";
import {TraceViewModel} from "../view/TraceViewModel.js";
import {TraceContainer} from "../view/TraceContainer.js";
import {TraceView} from "../view/TraceView.js";

/**
 * This class is responsible to manage PrintWizard
 */
export class PrintWizard {
    constructor() {
        /*this.breadcrumb = new Breadcrumb();
        this.breadcrumb.attachTo(document.querySelector('.breadcrumb'));
        this.jsonData = undefined;
        this.objectInspector = new ObjectInspector();
        this.objectInspector.attachTo(document.querySelector('#inspector'));
        this.searchInspector = new SearchInspector();
        this.searchInspector.attachTo(document.querySelector('#inspector'));
        this.trace = undefined;
        this.parser = undefined;*/
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
            const traceViewModel = new TraceViewModel(trace);
            const traceContainer = new TraceContainer(
                document.querySelector('.traceContent'),
                document.querySelector('.lineNumbers'),
                document.querySelector('.traceTriangles')
            );
            const traceView = new TraceView(traceViewModel, traceContainer);

            traceView.render();
        });

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
        });*/
    }

    /*
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
    }
    */
}
