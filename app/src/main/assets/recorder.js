// recorder.js

(function () {
    console.log("WebPursuer Recorder Injected");

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
    var lastHighlighted = null;

    window.enableSelectionMode = function () {
        selectionMode = true;
        console.log("Selection Mode Enabled");
    };

    window.disableSelectionMode = function () {
        selectionMode = false;
        if (lastHighlighted) {
            lastHighlighted.style.outline = "";
            lastHighlighted = null;
        }
        console.log("Selection Mode Disabled");
    };

    document.addEventListener('mouseover', function (e) {
        if (!selectionMode) return;
        var target = e.target;
        if (lastHighlighted && lastHighlighted !== target) {
            lastHighlighted.style.outline = "";
        }
        target.style.outline = "2px solid red";
        lastHighlighted = target;
    }, true);

    document.addEventListener('click', function (e) {
        if (selectionMode) {
            e.preventDefault();
            e.stopPropagation();
            var target = e.target;
            var selector = getCssSelector(target);
            console.log("Selected: " + selector);

            if (window.Android) {
                window.Android.elementSelected(selector);
            }
            // Disable selection mode after selection? Or keep it? Let's keep it until user confirms or cancels in App.
            // window.disableSelectionMode(); 
            return;
        }

        var target = e.target;
        var selector = getCssSelector(target);
        console.log("Clicked: " + selector);

        // Send to Android
        if (window.Android) {
            window.Android.recordInteraction("click", selector, "");
        }
    }, true); // Capture phase to catch it before other handlers if possible

    document.addEventListener('change', function (e) {
        if (selectionMode) return;

        var target = e.target;
        var selector = getCssSelector(target);
        var value = target.value;
        console.log("Changed: " + selector + " to " + value);

        if (window.Android) {
            window.Android.recordInteraction("input", selector, value);
        }
    }, true);

})();
