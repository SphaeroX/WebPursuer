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
    function querySelectorDeep(selector, root = document) {
        // Try shallow first for performance
        let el = root.querySelector(selector);
        if (el) return el;
        
        // Optimized deep search
        function findInShadows(currentRoot) {
            const all = currentRoot.querySelectorAll('*');
            for (let i = 0; i < all.length; i++) {
                const node = all[i];
                if (node.shadowRoot) {
                    const found = node.shadowRoot.querySelector(selector) || findInShadows(node.shadowRoot);
                    if (found) return found;
                }
            }
            return null;
        }
        return findInShadows(root);
    }

    function getRecursiveText(node) {
        if (node.nodeType === 3) { // Node.TEXT_NODE
            return (node.nodeValue || "").trim();
        }
        if (node.nodeType !== 1) return ""; // Node.ELEMENT_NODE

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

        var childTexts = [];
        
        // Traverse Shadow DOM if it exists
        if (node.shadowRoot) {
            for (var i = 0; i < node.shadowRoot.childNodes.length; i++) {
                var childVal = getRecursiveText(node.shadowRoot.childNodes[i]);
                if (childVal) childTexts.push(childVal);
            }
        }

        for (var i = 0; i < node.childNodes.length; i++) {
            var childVal = getRecursiveText(node.childNodes[i]);
            if (childVal) childTexts.push(childVal);
        }

        text = childTexts.join(isBlock ? "\n" : " ");
        if (isBlock) text = "\n" + text + "\n";
        return text;
    }

    function getSelectors(el) {
        if (!(el instanceof Node) || el.nodeType !== Node.ELEMENT_NODE) return [];
        var selectors = [];

        // 1. Stable Attribute Selector (highest priority for SPAs like OpenRouter)
        var stableAttrs = ['data-testid', 'aria-label', 'name', 'placeholder'];
        for (var i = 0; i < stableAttrs.length; i++) {
            var attr = stableAttrs[i];
            if (el.hasAttribute(attr)) {
                var val = el.getAttribute(attr);
                if (val && !val.includes("'")) {
                    selectors.push(el.tagName.toLowerCase() + "[" + attr + "='" + val + "']");
                }
            }
        }

        // 2. Text-based selector (if short and meaningful)
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

        // 3. Semantic XPath
        var tagName = el.tagName.toLowerCase();
        var xpathConditions = [];

        var isDynamicId = el.id && (el.id.includes("radix-") || /^:[a-zA-Z0-9_-]+:$/.test(el.id));
        if (el.id && !isDynamicId) {
            xpathConditions.push("@id='" + el.id + "'");
        }

        for (var i = 0; i < stableAttrs.length; i++) {
            var attr = stableAttrs[i];
            if (el.hasAttribute(attr)) {
                var val = el.getAttribute(attr);
                if (val && !val.includes("'")) {
                    xpathConditions.push("@" + attr + "='" + val + "'");
                }
            }
        }

        if (textContent.length > 0 && textContent.length < 50) {
            xpathConditions.push("normalize-space()='" + textContent.replace(/'/g, "\\'") + "'");
        }

        if (xpathConditions.length > 0) {
            selectors.push("xpath=//" + tagName + "[" + xpathConditions.join(" and ") + "]");
        }

        // 4. Fallback: Structural CSS selector (shorter version preferred)
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
                // Try classes if they don't look like tailwind hashes
                if (curr.classList.length > 0) {
                    var stableClass = Array.from(curr.classList).find(c => !/[0-9]/.test(c) && c.length > 3);
                    if (stableClass) {
                        sel += "." + stableClass;
                    }
                }
                var sib = curr, nth = 1;
                while (sib = sib.previousElementSibling) {
                    if (sib.nodeName.toLowerCase() == curr.nodeName.toLowerCase())
                        nth++;
                }
                if (nth != 1) sel += ":nth-of-type(" + nth + ")";
            }
            path.unshift(sel);
            curr = curr.parentNode;
            if (!curr && el.getRootNode() instanceof ShadowRoot) break;
            if (path.length > 5) break; // Don't make it too deep/brittle
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
                        el = querySelectorDeep(selector);
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
        if (!currentElement) return;
        var parent = currentElement.parentElement;
        if (!parent && currentElement.getRootNode) {
            var root = currentElement.getRootNode();
            if (root instanceof ShadowRoot) parent = root.host;
        }
        if (parent && parent.tagName && parent.tagName.toLowerCase() !== 'html' && parent.tagName.toLowerCase() !== 'body') {
            updateHighlight(parent);
        }
    };

    window.selectChild = function () {
        if (currentElement && currentElement.firstElementChild) {
            updateHighlight(currentElement.firstElementChild);
        }
    };

    window.getTextContent = function () {
        if (!currentElement) return "";
        // Clean up multiple newlines
        return getRecursiveText(currentElement).replace(/\n\s*\n/g, '\n').trim();
    };

    document.addEventListener('click', function (e) {
        if (selectionMode) {
            // Prevent navigation while selecting
            e.preventDefault();
            e.stopPropagation();

            // Update the selector/highlight for the clicked element
            updateHighlight(e.target);
            
            // Still record the interaction so navigation/clicks are captured in macros
            recordWithWait("click", e.target, "");
            
            return false;
        }

        // Recording logic
        var target = e.target;
        recordWithWait("click", target, "");
    }, true);

    document.addEventListener('input', function (e) {
        // Record input even in selection mode if the user is interacting with forms during navigation
        var target = e.target;
        var value = target.value;

        clearTimeout(inputTimeout);
        inputTimeout = setTimeout(function () {
            recordWithWait("input", target, value);
        }, 800);
    }, true);

    document.addEventListener('change', function (e) {
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
