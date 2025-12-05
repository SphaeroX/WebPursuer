// recorder.js

(function () {
    console.log("WebPursuer Recorder Injected");

    // --- CSS Styles ---
    var style = document.createElement('style');
    style.innerHTML = `
        .webpursuer-highlight {
            outline: 2px dashed #000 !important;
            background-color: rgba(173, 216, 230, 0.4) !important; /* Light blue */
            cursor: pointer !important;
        }
        .webpursuer-menu {
            position: absolute;
            z-index: 2147483647; /* Max z-index */
            background-color: #008f39; /* Green */
            color: white;
            border-radius: 5px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.3);
            font-family: sans-serif;
            font-size: 14px;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            min-width: 150px;
        }
        .webpursuer-menu-item {
            padding: 12px 16px;
            border-bottom: 1px solid rgba(255,255,255,0.2);
            cursor: pointer;
            white-space: nowrap;
            text-align: left;
        }
        .webpursuer-menu-item:last-child {
            border-bottom: none;
        }
        .webpursuer-menu-item:active {
            background-color: #006400;
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
    var menuElement = null;

    function createMenu() {
        if (menuElement) return;
        menuElement = document.createElement('div');
        menuElement.className = 'webpursuer-menu';

        var btnExpand = document.createElement('div');
        btnExpand.className = 'webpursuer-menu-item';
        btnExpand.innerText = 'Auswahl vergrößern';
        btnExpand.onclick = function (e) {
            e.stopPropagation();
            e.preventDefault();
            expandSelection();
        };

        var btnSimilar = document.createElement('div');
        btnSimilar.className = 'webpursuer-menu-item';
        btnSimilar.innerText = 'Ähnliches auswählen';
        btnSimilar.onclick = function (e) {
            e.stopPropagation();
            e.preventDefault();
            selectSimilar();
        };

        var btnConfirm = document.createElement('div');
        btnConfirm.className = 'webpursuer-menu-item';
        btnConfirm.innerText = 'Wähle dieses Element';
        btnConfirm.onclick = function (e) {
            e.stopPropagation();
            e.preventDefault();
            confirmSelection();
        };

        menuElement.appendChild(btnExpand);
        menuElement.appendChild(btnSimilar);
        menuElement.appendChild(btnConfirm);

        document.body.appendChild(menuElement);
    }

    function removeMenu() {
        if (menuElement && menuElement.parentNode) {
            menuElement.parentNode.removeChild(menuElement);
            menuElement = null;
        }
    }

    function updateHighlight(el) {
        // Remove old highlight
        var old = document.querySelector('.webpursuer-highlight');
        if (old) old.classList.remove('webpursuer-highlight');

        if (el) {
            el.classList.add('webpursuer-highlight');
            currentElement = el;
            showMenu(el);

            var selector = getCssSelector(el);
            if (window.Android) {
                window.Android.updateSelector(selector);
            }
        } else {
            removeMenu();
            currentElement = null;
            if (window.Android) {
                window.Android.updateSelector("");
            }
        }
    }

    function showMenu(el) {
        createMenu();
        var rect = el.getBoundingClientRect();
        var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        var scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;

        // Position below the element
        var top = rect.bottom + scrollTop + 10;
        var left = rect.left + scrollLeft;

        // Adjust if off screen
        if (left + menuElement.offsetWidth > window.innerWidth) {
            left = window.innerWidth - menuElement.offsetWidth - 10;
        }
        if (left < 0) left = 10;

        menuElement.style.top = top + 'px';
        menuElement.style.left = left + 'px';
    }

    function expandSelection() {
        if (currentElement && currentElement.parentElement) {
            // Don't go beyond body
            if (currentElement.parentElement.tagName.toLowerCase() !== 'html') {
                updateHighlight(currentElement.parentElement);
            }
        }
    }

    function selectSimilar() {
        // Placeholder: Select siblings with same tag and class
        // For now, just a toast or log
        console.log("Select similar clicked");
        // Implementing a basic "similar" check
        if (!currentElement) return;

        // Logic to select similar elements is complex to visualize without multi-selection support in UI.
        // For now, we will just keep the current element selected.
    }

    function confirmSelection() {
        if (currentElement && window.Android) {
            var selector = getCssSelector(currentElement);
            window.Android.confirmSelection(selector);
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
                // Optionally remove highlight if not found, or keep last valid
                // updateHighlight(null); 
                console.log("Element not found for selector: " + selector);
            }
        } catch (e) {
            console.error("Invalid selector: " + selector);
        }
    };

    document.addEventListener('click', function (e) {
        if (selectionMode) {
            // Check if clicking inside the menu
            if (e.target.closest('.webpursuer-menu')) {
                return;
            }

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
