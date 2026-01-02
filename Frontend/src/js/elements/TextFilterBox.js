import {PWElement} from "./PWElement.js";

/**
 * Represents a text filter box. A text input that can be used to filter items based on user input.
 */
export class TextFilterBox extends PWElement {
    constructor({placeholder = "Filter (ex: 10, v1, v>=3, >=100)", className = "", debounceMs = 0} = {}) {
        const input = document.createElement("input");
        input.type = "text";
        input.placeholder = placeholder;
        input.autocomplete = "off";
        input.spellcheck = false;

        if (className) input.classList.add(className);

        super(input);

        this._timer = null;
        this._debounceMs = debounceMs;
    }

    get value() {
        return this.element.value ?? "";
    }

    set value(v) {
        this.element.value = v ?? "";
    }

    onChange(cb) {
        const handler = () => cb(this.value);

        this.element.addEventListener("input", () => {
            if (!this._debounceMs) return handler();
            clearTimeout(this._timer);
            this._timer = setTimeout(handler, this._debounceMs);
        });

        this.element.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                this.value = "";
                cb("");
            }
        });
    }
}
