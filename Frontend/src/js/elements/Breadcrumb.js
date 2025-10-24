import {PWElement} from "./PWElement.js";

/**
 * This class is used to manage a breadcrumb list, to show the current filter
 */
export class Breadcrumb extends PWElement {

    /**
     * Creates a new breadcrumb list
     */
    constructor(TraceViewModel) {
        super(document.createElement('ul'));
        this.element.classList.add('breadcrumb');

        this.vm = TraceViewModel;
    }

    render() {
        const filterType = this.vm.filterType;
        const filterId = this.vm.filterId;
        const segments = this.#computeSegments(filterType, filterId);
        this.#renderSegments(segments);
    }

    #renderSegments(segments) {
        this.element.innerHTML = "";

        for (const seg of segments) {
            const li = document.createElement("li");

            li.textContent = seg.label;

            this.element.append(li);
        }
    }

    #computeSegments(type, id) {
        switch (type.name) {
            case "span": {
                const segments = [];
                const method = this.vm.getCurrentMethod();
                const clazz = this.vm.getClass(method.classId);

                segments.push({ label: `${clazz.name}:${method.name}` });

                return segments;
            }

            default:
                return [{ label: `${type.name}:${id}` }];
        }
    }
}
