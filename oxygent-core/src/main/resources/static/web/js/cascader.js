/**
 * Cascader Component
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

// Cascader constructor function
function Cascader(config) {
    this.inputEl = document.querySelector(config.el);
    this.dropdownEl = document.querySelector(config.dropdown);
    this.options = config.options || [];
    this.onChange = config.onChange || function () {
    };
    this.selectedValues = [];
    this.selectedOptions = [];

    this.init();
}

Cascader.prototype.updateOptions = function(options) {
    this.options = options;
    this.selectedValues = [];
    this.selectedOptions = [];
};

Cascader.prototype.destroy = function() {
    if (this.$inputEl) {
        this.$inputEl.remove();
        this.$inputEl = null;
    }
};

// Initialize method
Cascader.prototype.init = function () {
    this.renderInput();
    this.bindEvents();
};

// Render input box
Cascader.prototype.renderInput = function () {
    var placeholder = this.inputEl.querySelector('.placeholder');
    // if (this.selectedOptions.length > 0) {
    //     var labels = this.selectedOptions.map(function (opt) {
    //         return opt.label;
    //     });
    //     placeholder.textContent = labels.join(' / ');
    //     placeholder.style.color = '#333';
    // } else {
    //     placeholder.textContent = 'Please select';
    //     placeholder.style.color = '#999';
    // }
};

// Bind events
Cascader.prototype.bindEvents = function () {
    var self = this;

    // Input box click event
    this.inputEl.addEventListener('click', function (e) {
        e.stopPropagation();
        self.toggleDropdown();
    });

    // Document click event to close dropdown menu
    document.addEventListener('click', function () {
        self.hideDropdown();
    });
};

// Toggle dropdown menu show/hide
Cascader.prototype.toggleDropdown = function () {
    if (this.dropdownEl.classList.contains('show')) {
        this.hideDropdown();
    } else {
        this.showDropdown();
    }
};

// Show dropdown menu
Cascader.prototype.showDropdown = function () {
    this.dropdownEl.classList.add('show');
    this.inputEl.querySelector('.arrow').classList.add('open');
    this.renderMenus();
};

// Hide dropdown menu
Cascader.prototype.hideDropdown = function () {
    this.dropdownEl.classList.remove('show');
    this.inputEl.querySelector('.arrow').classList.remove('open');
};

// Render cascader menu
Cascader.prototype.renderMenus = function () {
    var menusContainer = this.dropdownEl.querySelector('.cascader-menus');
    menusContainer.innerHTML = '';

    // If no item is selected, only show first level menu
    if (this.selectedValues.length === 0) {
        var menu = this.createMenu(this.options, 0);
        menusContainer.appendChild(menu);
        return;
    }

    // Show selected level menus
    var currentOptions = this.options;
    for (var i = 0; i < this.selectedValues.length; i++) {
        var menu = this.createMenu(currentOptions, i);
        menusContainer.appendChild(menu);

        // Highlight selected options
        var selectedValue = this.selectedValues[i];
        var optionEls = menu.querySelectorAll('.cascader-option');
        for (var j = 0; j < optionEls.length; j++) {
            if (optionEls[j].dataset.value === selectedValue) {
                optionEls[j].classList.add('selected');
            }
        }

        // Find next level options
        var selectedOption = null;
        for (var k = 0; k < currentOptions.length; k++) {
            if (currentOptions[k].value === selectedValue) {
                selectedOption = currentOptions[k];
                break;
            }
        }

        if (selectedOption && selectedOption.children) {
            currentOptions = selectedOption.children;
        } else {
            currentOptions = [];
        }
    }

    // If has children, show next level menu
    if (currentOptions && currentOptions.length > 0) {
        console.log(currentOptions);
        var nextMenu = this.createMenu(currentOptions, this.selectedValues.length);
        menusContainer.appendChild(nextMenu);
    }
};

// Create single menu
Cascader.prototype.createMenu = function (options, level) {
    var menu = document.createElement('div');
    menu.className = 'cascader-menu';

    var self = this;

    options.forEach(function (option) {
        var optionEl = document.createElement('div');
        var icon = option.image ? `<img src="${option.image}" />` : '';


        optionEl.id = option.id;
        optionEl.className = 'cascader-option';

        optionEl.dataset.value = option.value;
        optionEl.dataset.level = level;

        optionEl.innerHTML = icon + option.label +
            (option.children ? '<img alt="#" src="./image/arrow-right.svg" class="arrow"></img>' : '');

        optionEl.addEventListener('click', function (e) {
            e.stopPropagation();
            self.handleOptionClick(option, level);
        });

        menu.appendChild(optionEl);
    });

    return menu;
};


// Handle option click
Cascader.prototype.handleOptionClick = function (option, level) {
    console.log(option);
    // Update selected values
    this.selectedValues = this.selectedValues.slice(0, level);
    this.selectedOptions = this.selectedOptions.slice(0, level);

    this.selectedValues.push(option.value);
    this.selectedOptions.push({
        value: option.value,
        label: option.label
    });

    // If no children, close dropdown menu
    if (!option.children || option.children.length === 0) {
        this.hideDropdown();
        this.renderInput();
        this.onChange(this.selectedValues, this.selectedOptions);
        this.selectedValues = [];
        return;
    }

    // Re-render menus
    this.renderMenus();
};




