import {PWElement} from "./PWElement.js";
import {Preconditions} from "../utils/Preconditions.js";

/**
 * This class is used to manage a breadcrumb list
 */
export class Breadcrumb extends PWElement {

    /**
     * Creates a new breadcrumb list
     * @param traceViewModel {TraceViewModel}
     */
    constructor(traceViewModel) {
        super(document.createElement('ul'));
        this.element.classList.add('breadcrumb');
        this.traceViewModel = traceViewModel;
    }

    setFromFilter() {
        const filterType = this.traceViewModel.filterType;
        const filterId = this.traceViewModel.filterId;
    }
}
