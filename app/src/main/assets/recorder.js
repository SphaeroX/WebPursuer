// recorder.js

(function () {
    if (window.WebPursuerRecorderInjected) {
        console.log("WebPursuer Recorder already injected");
        return;
    }
    window.WebPursuerRecorderInjected = true;
    console.log("WebPursuer Recorder Injected");

    // --- CSS Styles ---
    var style = document.createElement('style');
    style.innerHTML = `
        .webpursuer-highlight {
            outline: 2px dashed #000 !important;
            background-color: rgba(173, 216, 230, 0.4) !important; /* Light blue */
            cursor: pointer !important;
        }
    `;
    document.head.appendChild(style);

    // --- Helper Functions ---
    function getCssSelector(el) {
        if (!(el instanceof Element)) return;
        var path = [];
        while (el.nodeType === Node.ELEMENT_NODE) {
            var selector = el.nodeName.toLowerCase();
            if (el.id) {
                selector += '#' + el.id;
                path.unshift(selector);
                break;
            } else {
                var sib = el, nth = 1;
                while (sib = sib.previousElementSibling) {
                    if (sib.nodeName.toLowerCase() == selector)
                        nth++;
                }
                if (nth != 1)
                    selector += ":nth-of-type(" + nth + ")";
            }
            path.unshift(selector);
            el = el.parentNode;
        }
        return path.join(" > ");
    }

    var selectionMode = false;
    var currentElement = null;

    function updateHighlight(el) {
        // Remove old highlight
        var old = document.querySelector('.webpursuer-highlight');
        if (old) old.classList.remove('webpursuer-highlight');

        if (el) {
            el.classList.add('webpursuer-highlight');
            currentElement = el;

            var selector = getCssSelector(el);
            if (window.Android) {
                window.Android.updateSelector(selector);
            }
        } else {
            currentElement = null;
            if (window.Android) {
                window.Android.updateSelector("");
            }
        }
    }

    window.enableSelectionMode = function () {
        selectionMode = true;
        console.log("Selection Mode Enabled");
    };

    window.disableSelectionMode = function () {
        selectionMode = false;
        updateHighlight(null);
        console.log("Selection Mode Disabled");
    };

    window.highlightSelector = function (selector) {
        try {
            var el = document.querySelector(selector);
            if (el) {
                updateHighlight(el);
                // Scroll into view if needed
                el.scrollIntoView({ behavior: "smooth", block: "center", inline: "nearest" });
            } else {
                console.log("Element not found for selector: " + selector);
            }
        } catch (e) {
            console.error("Invalid selector: " + selector);
        }
    };

    window.selectParent = function () {
        if (currentElement && currentElement.parentElement) {
            if (currentElement.parentElement.tagName.toLowerCase() !== 'html') {
                updateHighlight(currentElement.parentElement);
            }
        }
    };

    window.selectChild = function () {
        if (currentElement && currentElement.firstElementChild) {
            updateHighlight(currentElement.firstElementChild);
        }
    };

    window.getTextContent = function () {
        if (!currentElement) return "";
        return currentElement.value || currentElement.innerText || currentElement.textContent || "";
    };

    document.addEventListener('click', function (e) {
        if (selectionMode) {
            e.preventDefault();
            e.stopPropagation();

            updateHighlight(e.target);
            return;
        }

        // Recording logic
        var target = e.target;
        var selector = getCssSelector(target);
        if (window.Android) {
            window.Android.recordInteraction("click", selector, "");
        }
    }, true);

    document.addEventListener('change', function (e) {
        if (selectionMode) return;

        var target = e.target;
        var selector = getCssSelector(target);
        var value = target.value;

        if (window.Android) {
            window.Android.recordInteraction("input", selector, value);
        }
    }, true);

})();
