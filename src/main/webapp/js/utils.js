/**
 *
 * @type {{formToJson, staticMethod}}
 */
var Utils = (function() {

    function animate($el, classAnimation) {
        $el.addClass(classAnimation + ' animated').one('webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend', function(){
            $(this).removeClass(classAnimation + ' animated');
        });
    }

    function serializeForm($form) {
        var obj = {};
        var array = $form.serializeArray();
        var pattern = /[a-z0-9_]+|(?=\[\])/gi; // This is magic. I found it on StackOverflow lol.
        for(var i = 0; i < array.length; i++) {
            var keys = (array[i].name).match(pattern); // Transform 'toto[tata][blublu]' into ['toto','tata','blublu']
            var curObj = obj;
            var key;
            while((key = keys.shift()) !== undefined) {
                if(keys.length === 0) {
                    curObj[key] = array[i].value;
                } else {
                    if(!curObj.hasOwnProperty(key)) {
                        curObj[key] = {};
                    }
                    curObj = curObj[key];
                }
            }
        }
        return obj;
    }

    function populateForm($form, obj) {
        var inputs = $form.find(':input');
        var pattern = /[a-z0-9_]+|(?=\[\])/gi;
        for(var i = 0; i < inputs.length; i++) {
            var name = $(inputs[i]).attr('name');
            if (name === undefined) {
                continue;
            }
            var keys = name.match(pattern);
            var curObj = obj;
            var undefinedProperty = false;
            var key;
            while((key = keys.shift()) !== undefined) {
                if(!curObj.hasOwnProperty(key)) {
                    undefinedProperty = true;
                    break;
                }
                curObj = curObj[key];
            }
            if(!undefinedProperty) {
                populateInput($(inputs[i]), curObj);
            }
        }
    }

    function populateInput($input, value) {
        if ($input.is(':radio')) {
            if($input.val() === value) {
                $input.prop('checked', true);
            }
        } else if($input.is('select')) {
            $input.attr('selected', false);
            $input.find('[value="' + value + '"]').prop('selected', true);
        } else {
            $input.val(value);
        }
    }

    function departureStr(term, academicYear) {
        return ((term === 1) ? 'Septembre' : 'Février') + ' ' + academicYear;
    }

    function handleStateClass(state) {
        switch(state) {
            case 'Créée':
                return 'state-created';
            case 'En préparation':
                return 'state-in-preparation';
            case 'A payer':
                return 'state-to-be-paid';
            case 'En cours':
                return 'state-in-progress';
            case 'Solde à payer':
                return 'state-balance-to-be-paid';
            case 'Terminée':
                return 'state-closed';
            default:
                return 'state-cancelled';
        }
    }

    /**
     * The RFC 9457 problem of a failed API call (jqXHR), normalised to
     * {status, code, title, detail, errors}. The API answers every error with
     * application/problem+json: `detail` and `errors[].message` are already
     * localized by the server (Accept-Language), `code` is the stable key to
     * branch on (USERNAME_TAKEN, VALIDATION_FAILED...). A generic message is
     * used when the response is not a problem (network error, proxy page...).
     */
    function problemOf(xhr) {
        var body = xhr ? xhr.responseJSON : undefined;
        if (body === undefined && xhr && xhr.responseText
            && /json/i.test(xhr.getResponseHeader('Content-Type') || '')) {
            try {
                body = JSON.parse(xhr.responseText);
            } catch (e) {
                body = undefined;
            }
        }
        var problem = (body !== null && typeof body === 'object') ? body : {};
        var status = xhr ? xhr.status : 0;
        return {
            status: status,
            code: typeof problem.code === 'string' ? problem.code : 'UNKNOWN',
            title: typeof problem.title === 'string' ? problem.title : 'Erreur',
            detail: typeof problem.detail === 'string' ? problem.detail
                : (status === 0 ? 'Impossible de joindre le serveur. Vérifiez votre connexion.'
                    : 'Une erreur inattendue est survenue. Veuillez réessayer plus tard.'),
            errors: $.isArray(problem.errors) ? problem.errors : []
        };
    }

    /**
     * Name of the form input of a problem field, following the naming of
     * serializeForm: 'address.country.countryCode' -> 'address[country][countryCode]'.
     */
    function inputName(field) {
        var parts = String(field).replace(/\[(\d+)\]/g, '.$1').split('.');
        return parts[0] + (parts.length > 1 ? '[' + parts.slice(1).join('][') + ']' : '');
    }

    /**
     * Shows the field errors of a problem under the inputs of a form (same
     * markup as jquery.validate: label.error). Returns the errors matching no
     * input of the form.
     */
    function showFieldErrors($form, errors) {
        $form.find('label.error.problem-error').remove();
        var unmatched = [];
        for (var i = 0; i < errors.length; i++) {
            var $input = $form.find('[name="' + inputName(errors[i].field) + '"]').last();
            if ($input.length === 0) {
                unmatched.push(errors[i]);
                continue;
            }
            $('<label class="error problem-error"></label>')
                .attr('for', $input.attr('id') || '')
                .text(errors[i].message)
                .appendTo($input.parent());
        }
        return unmatched;
    }

    // Public API
    // A mobility (or payment) may come from a choice without country or partner: the API then
    // leaves the property out (null properties are not serialized). "== null" also covers an
    // explicit null.
    function flag(country) {
        if (country == null || country.countryCode == null) {
            return '';
        }
        return '<img src="/images/flags/' + country.countryCode + '.png" class="flags" alt="'
            + (country.name == null ? 'Drapeau' : country.name) + '">';
    }

    function countryName(country) {
        return (country == null || country.name == null) ? '' : country.name;
    }

    function partnerName(partner) {
        return (partner == null || partner.fullName == null) ? '' : partner.fullName;
    }

    return {
        animate: animate,
        flag: flag,
        countryName: countryName,
        partnerName: partnerName,
        serializeForm: serializeForm,
        populateForm: populateForm,
        departureStr: departureStr,
        handleStateClass: handleStateClass,
        problemOf: problemOf,
        inputName: inputName,
        showFieldErrors: showFieldErrors
    }

})();