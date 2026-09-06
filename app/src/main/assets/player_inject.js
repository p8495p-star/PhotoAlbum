(function () {
  if (window.__photoAlbumPlayerInjected) return;
  window.__photoAlbumPlayerInjected = true;

  function openNative(url) {
    if (!url) return false;
    try {
      if (window.PhotoAlbumBridge) {
        window.PhotoAlbumBridge.openVideo(url);
        return true;
      }
    } catch (e) {
      return false;
    }
    return false;
  }

  var origPlay = HTMLMediaElement.prototype.play;

  HTMLMediaElement.prototype.play = function () {
    var v = this;
    var u = v.currentSrc || v.src;
    if (u && !/^blob:/i.test(u)) {
      if (openNative(u)) return Promise.resolve();
    }
    return origPlay.apply(v, arguments);
  };

  window.PhotoAlbumPlayer = {
    open: function (url) {
      return openNative(url);
    }
  };

  window.__photoAlbumVideoFinished = function () {
    try {
      window.dispatchEvent(new CustomEvent('photoalbum-video-end'));
    } catch (e) {}
  };
})();