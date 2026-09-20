/* Campus Placement and Training Cell
   ----------------------------------
   The only script in the application, loaded with defer from every page.

   It exists so that no page needs an inline <script> block or an onclick
   attribute. That is not tidiness for its own sake: with no inline script
   anywhere, the Content-Security-Policy header can say script-src 'self' with
   no 'unsafe-inline', and an injected <script> in any field on any page simply
   does not execute. An inline handler in one JSP would force that policy to be
   relaxed for every page at once. */

(function () {
    'use strict';

    /* ---------------------------------------------------------------
       Destructive actions ask first.

       A form carrying data-confirm gets a confirmation dialogue. The
       message lives in the markup where the action is, and the wiring
       lives here.
       --------------------------------------------------------------- */
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        var message = form.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            event.preventDefault();
        }
    });

    /* ---------------------------------------------------------------
       Bulk import.

       Streams the pasted rows to the non blocking servlet and renders
       the JSON summary. Every value that came back from the server is
       written with textContent, never innerHTML, so a name in the CSV
       cannot inject markup into this page.
       --------------------------------------------------------------- */
    var runButton = document.getElementById('run');
    if (!runButton) {
        return;
    }

    var state = document.getElementById('state');
    var card = document.getElementById('resultCard');
    var box = document.getElementById('result');
    var endpoint = runButton.getAttribute('data-endpoint');
    var registerUrl = runButton.getAttribute('data-register-url');

    function element(tag, className, text) {
        var node = document.createElement(tag);
        if (className) {
            node.className = className;
        }
        if (text !== undefined && text !== null) {
            node.textContent = text;
        }
        return node;
    }

    function alertBox(kind, iconName, text) {
        var wrapper = element('div', 'alert alert-' + kind);
        var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('class', 'i');
        var use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
        use.setAttribute('href', '#i-' + iconName);
        svg.appendChild(use);
        wrapper.appendChild(svg);
        wrapper.appendChild(element('div', null, text));
        return wrapper;
    }

    function render(data) {
        box.textContent = '';

        if (data.created > 0) {
            var created = data.created + ' student(s) created';
            if (data.rolls && data.rolls.length) {
                created += ': ' + data.rolls.join(', ');
            }
            box.appendChild(alertBox('ok', 'check', created));
        } else {
            box.appendChild(alertBox('warn', 'alert', 'No students were created.'));
        }

        if (data.skipped && data.skipped.length) {
            box.appendChild(element('h4', null,
                data.skipped.length + ' row(s) refused'));
            var list = element('ul', 'reasons');
            data.skipped.forEach(function (reason) {
                list.appendChild(element('li', null, reason));
            });
            box.appendChild(list);
        }

        if (registerUrl) {
            var footer = element('p', 'small mt-2');
            var link = element('a', null, 'Open the student register');
            link.href = registerUrl;
            footer.appendChild(link);
            box.appendChild(footer);
        }

        card.style.display = '';
    }

    runButton.addEventListener('click', function () {
        var body = document.getElementById('csv').value;
        runButton.disabled = true;
        state.textContent = 'Streaming the rows to the server...';

        fetch(endpoint, {
            method: 'POST',
            headers: {'Content-Type': 'text/csv; charset=utf-8'},
            body: body
        }).then(function (response) {
            if (!response.ok && response.status !== 400) {
                throw new Error('the server answered ' + response.status);
            }
            return response.json();
        }).then(function (data) {
            render(data);
            state.textContent = '';
            runButton.disabled = false;
        }).catch(function (error) {
            box.textContent = '';
            box.appendChild(alertBox('bad', 'alert',
                'The import could not be completed: ' + error.message));
            card.style.display = '';
            state.textContent = '';
            runButton.disabled = false;
        });
    });
})();
