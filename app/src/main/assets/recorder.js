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
    function getSelectors(el) {
        if (!(el instanceof Node) || el.nodeType !== Node.ELEMENT_NODE) return [];
        var selectors = [];

        // 1. Text-based selector (if short and meaningful)
        var textContent = "";
        for (var i = 0; i < el.childNodes.length; i++) {
            if (el.childNodes[i].nodeType === Node.TEXT_NODE) {
                textContent += el.childNodes[i].nodeValue;
            }
        }
        textContent = textContent.trim();
        if (textContent.length > 0 && textContent.length < 100) {
            selectors.push("text=" + textContent);
        }

        // 2. Semantic XPath
        var tagName = el.tagName.toLowerCase();
        var xpathConditions = [];

        var isDynamicId = el.id && (el.id.includes("radix-") || /^:[a-zA-Z0-9_-]+:$/.test(el.id));
        if (el.id && !isDynamicId) {
            xpathConditions.push("@id='" + el.id + "'");
        }

        var attrs = ['name', 'placeholder', 'role', 'aria-label', 'data-testid'];
        for (var i = 0; i < attrs.length; i++) {
            var attr = attrs[i];
            if (el.hasAttribute(attr)) {
                var val = el.getAttribute(attr);
                if (val && !val.includes("'")) { // safety against quotes
                    xpathConditions.push("@" + attr + "='" + val + "'");
                }
            }
        }

        if (textContent.length > 0 && textContent.length < 100) {
            xpathConditions.push("normalize-space()='" + textContent.replace(/'/g, "\\'") + "'");
        }

        if (xpathConditions.length > 0) {
            var xpath = "xpath=//" + tagName + "[" + xpathConditions.join(" and ") + "]";
            selectors.push(xpath);
        }

        // 3. Fallback: Full CSS selector
        var path = [];
        var curr = el;
        while (curr && curr.nodeType === Node.ELEMENT_NODE) {
            var sel = curr.nodeName.toLowerCase();
            var dynamicId = curr.id && (curr.id.includes("radix-") || /^:[a-zA-Z0-9_-]+:$/.test(curr.id));
            if (curr.id && !dynamicId) {
                sel += '#' + curr.id;
                path.unshift(sel);
                break;
            } else {
                var sib = curr, nth = 1;
                while (sib = sib.previousElementSibling) {
                    if (sib.nodeName.toLowerCase() == sel)
                        nth++;
                }
                if (nth != 1)
                    sel += ":nth-of-type(" + nth + ")";
            }
            path.unshift(sel);
            curr = curr.parentNode;
        }
        if (path.length > 0) {
            selectors.push(path.join(" > "));
        }

        return selectors;
    }

    var selectionMode = false;
    var currentElement = null;
    var lastInteractionTime = Date.now();
    var inputTimeout = null;
    var scrollTimeout = null;

    function recordWithWait(type, targetElement, value) {
        if (!window.Android) return;

        var selectorStr = "";
        if (targetElement) {
            var selectors = getSelectors(targetElement);
            selectorStr = JSON.stringify(selectors);
        } else if (type === "scroll" || type === "wait") {
            selectorStr = "window"; // Or empty string for wait
        }

        var now = Date.now();
        var delay = now - lastInteractionTime;
        // Record wait if there was a pause of more than 500ms
        if (delay > 500) {
            window.Android.recordInteraction("wait", "", delay.toString());
        }
        lastInteractionTime = now;
        window.Android.recordInteraction(type, selectorStr, value);
    }

    function updateHighlight(el) {
        // Remove old highlight
        var old = document.querySelector('.webpursuer-highlight');
        if (old) old.classList.remove('webpursuer-highlight');

        if (el) {
            el.classList.add('webpursuer-highlight');
            currentElement = el;

            var selectors = getSelectors(el);
            if (window.Android) {
                window.Android.updateSelector(JSON.stringify(selectors));
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

    window.highlightSelector = function (selectorStr) {
        if (!selectorStr) return;
        try {
            var selectors = [];
            try {
                var parsed = JSON.parse(selectorStr);
                if (Array.isArray(parsed)) selectors = parsed;
                else selectors = [selectorStr];
            } catch (e) {
                selectors = [selectorStr];
            }

            var el = null;
            for (var i = 0; i < selectors.length; i++) {
                var selector = selectors[i];
                try {
                    if (selector.startsWith('xpath=')) {
                        el = document.evaluate(selector.substring(6), document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                    } else if (selector.startsWith('text=')) {
                        var xpath = "//*[normalize-space()='" + selector.substring(5).replace(/'/g, "\\'") + "']";
                        el = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                    } else {
                        el = document.querySelector(selector);
                    }
                } catch (e) { }
                if (el) break; // First working selector
            }

            if (el) {
                updateHighlight(el);
                // Scroll into view if needed
                el.scrollIntoView({ behavior: "smooth", block: "center", inline: "nearest" });
            } else {
                console.log("Element not found for selector(s): " + selectorStr);
            }
        } catch (e) {
            console.error("Invalid selector matching: " + selectorStr);
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

        function getRecursiveText(node) {
            if (node.nodeType === Node.TEXT_NODE) {
                return (node.nodeValue || "").trim();
            }
            if (node.nodeType !== Node.ELEMENT_NODE) return "";

            var tagName = node.tagName.toLowerCase();
            // Skip scripts and styles
            if (tagName === 'script' || tagName === 'style' || tagName === 'noscript') return "";

            var text = "";
            var isBlock = false;
            try {
                var style = window.getComputedStyle(node);
                // Skip hidden elements, but leniently
                if (style.display === 'none' || style.visibility === 'hidden') return "";
                isBlock = (style.display === 'block' || style.display === 'flex' || style.display === 'grid' || style.display === 'table-row');
            } catch (e) { }

            // Form elements
            if (tagName === 'input') {
                var type = node.type ? node.type.toLowerCase() : 'text';
                if (type !== 'hidden' && type !== 'submit' && type !== 'button' && type !== 'image') {
                    return node.value || "";
                }
            }
            if (tagName === 'textarea') {
                return node.value || "";
            }
            if (tagName === 'select') {
                if (node.selectedIndex >= 0) return node.options[node.selectedIndex].text;
                return "";
            }
            if (tagName === 'br') return "\n";

            // Children
            var childTexts = [];
            for (var i = 0; i < node.childNodes.length; i++) {
                var childVal = getRecursiveText(node.childNodes[i]);
                if (childVal) childTexts.push(childVal);
            }

            text = childTexts.join(isBlock ? "\n" : " ");

            if (isBlock) text = "\n" + text + "\n";

            return text;
        }

        // Clean up multiple newlines
        return getRecursiveText(currentElement).replace(/\n\s*\n/g, '\n').trim();
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
        recordWithWait("click", target, "");
    }, true);

    document.addEventListener('input', function (e) {
        if (selectionMode) return;

        var target = e.target;
        var value = target.value;

        clearTimeout(inputTimeout);
        inputTimeout = setTimeout(function () {
            recordWithWait("input", target, value);
        }, 800);
    }, true);

    document.addEventListener('change', function (e) {
        if (selectionMode) return;

        var target = e.target;
        var value = target.value;

        clearTimeout(inputTimeout);
        recordWithWait("input", target, value);
    }, true);

    window.addEventListener('scroll', function (e) {
        if (selectionMode) return;

        clearTimeout(scrollTimeout);
        scrollTimeout = setTimeout(function () {
            var scrollPos = window.scrollY || document.documentElement.scrollTop;
            recordWithWait("scroll", null, scrollPos.toString());
        }, 500);
    }, true);

})();
