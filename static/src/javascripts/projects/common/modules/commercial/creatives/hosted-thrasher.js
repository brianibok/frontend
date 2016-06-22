define([
    'common/utils/fastdom-promise',
    'common/utils/config',
    'common/utils/template',
    'common/modules/commercial/hosted-video',
    'common/modules/commercial/creatives/add-tracking-pixel',
    'text!common/views/commercial/creatives/hosted-thrasher.html'
], function (
    fastdom,
    config,
    template,
    hostedVideo,
    addTrackingPixel,
    hostedThrasherStr
) {
    var hostedThrasherTemplate;

    var HostedThrasher = function ($adSlot, params) {
        this.$adSlot = $adSlot;
        this.params = params;
    };

    HostedThrasher.prototype.create = function () {
        hostedThrasherTemplate = hostedThrasherTemplate || template(hostedThrasherStr);

        return fastdom.write(function () {
            var title = this.params.header2 || 'unknown';
            var sponsor = 'Renault';
            var videoLength = this.params.videoLength;
            if(videoLength){
                var seconds = videoLength % 60;
                var minutes = (videoLength - seconds) / 60;
                this.params.timeString = minutes + (seconds < 10 ? ':0' : ':') + seconds;
            }

            this.params.linkTracking = 'Labs hosted container' +
                ' | ' + config.page.edition +
                ' | ' + config.page.section +
                ' | ' + title +
                ' | ' + sponsor;
            this.$adSlot.append(hostedThrasherTemplate({ data: this.params }));
            if (this.params.trackingPixel) {
                addTrackingPixel(this.$adSlot, this.params.trackingPixel + this.params.cacheBuster);
            }
        }, this).then(hostedVideo.init).then(function () {
            return true;
        });
    };

    return HostedThrasher;

});
