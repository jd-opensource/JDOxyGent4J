/**
 * Simple message implementation
 * Message SDK
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
(function($) {
    // Define Message class
    function Message() {
        this.$container = null;
    }

    // Initialize message container
    Message.prototype.init = function() {
         this.$container = $('<div class="message-sdk" style="position: fixed; top: 20px; left: 50%; transform: translateX(-50%); width: 200px; padding: 10px; background: #333; color: white; text-align: center; border-radius: 4px; z-index: 9999;"></div>');
        
        $('body').append(this.$container);
        return this;
    };

    // Show message
    Message.prototype.show = function(content) {
        if (!this.$container) {
            this.init();
        }
        this.$container.html(content).fadeIn(200);
        
        // Auto disappear after 100ms
        const self = this;
        setTimeout(function() {
            self.$container.fadeOut(200, function() {
                // Destroy after animation completes
                self.destroy();
            });
        }, 500);
    };

    // Destroy message container
    Message.prototype.destroy = function() {
        if (this.$container) {
            this.$container.remove();
            this.$container = null;
        }
    };

    // Exposed API
    window.MessageSDK = {
        show: function(content) {
            const msg = new Message();
            msg.show(content);
        }
    };
})(jQuery);

