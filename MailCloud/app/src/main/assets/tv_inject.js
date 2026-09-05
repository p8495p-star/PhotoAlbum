(function(){
  if (window.__tvUI) return;
  window.__tvUI = true;

  var style = document.createElement('style');
  style.id = '__tvUiStyle';
  style.textContent = '*:focus{outline:3px solid #FFD740 !important;outline-offset:2px !important;}';
  var head = document.head || document.documentElement;
  head.appendChild(style);

  function isVis(e){
    if (!e || e === document.body || e === document.documentElement) return true;
    try {
      var cs = window.getComputedStyle(e);
      if (cs.display === 'none' || cs.visibility === 'hidden') return false;
    } catch (err) { }
    var r = e.getBoundingClientRect();
    return r.width > 2 && r.height > 2;
  }

  function findOverlay(){
    var w = window.innerWidth, h = window.innerHeight;
    var strong = '[role="dialog"],[role="presentation"],[class*="viewer"],[class*="lightbox"],[class*="preview"],[class*="fullscreen"],[class*="modal"],[class*="zoom"],[class*="gallery"],[class*="slideshow"],[class*="Viewer"],[class*="Lightbox"]';
    var base = document.querySelectorAll(strong);
    for (var i = 0; i < base.length; i++) {
      var e = base[i];
      if (!isVis(e)) continue;
      var r = e.getBoundingClientRect();
      if (r.width > w * 0.4 && r.height > h * 0.3) return e;
    }
    var all = document.querySelectorAll('div,section,main,article');
    var best = null, bestA = 0;
    for (i = 0; i < all.length; i++) {
      e = all[i];
      if (!isVis(e)) continue;
      var cs = window.getComputedStyle(e);
      if (cs.position !== 'fixed' && cs.position !== 'absolute') continue;
      r = e.getBoundingClientRect();
      var a = r.width * r.height;
      if (a > bestA) { bestA = a; best = e; }
    }
    if (bestA > w * h * 0.55) return best;
    return null;
  }

  function findArrow(ov, dir){
    var leftWords = ['предыдущ','пред','previous','prev','назад','left','влево','arrow_left','arrow-left','navigate_before','chevron_left','chevron-left'];
    var rightWords = ['следующ','след','next','далее','forward','right','вправо','arrow_right','arrow-right','navigate_next','chevron_right','chevron-right'];
    var words = dir < 0 ? leftWords : rightWords;
    var list = ov ? ov.querySelectorAll('a[href],button,[role="button"],[class*=arrow],[class*=chevron],[class*=nav],[class*=control]') : [];
    for (var i = 0; i < list.length; i++) {
      var el = list[i];
      if (!isVis(el)) continue;
      var text = (el.getAttribute && (el.getAttribute('aria-label') || '')) + ' ' + (el.textContent || '');
      var cls = (el.className && (typeof el.className === 'string' ? el.className : '')) || '';
      var blob = (text + ' ' + cls).toLowerCase();
      for (var k = 0; k < words.length; k++) {
        if (blob.indexOf(words[k]) >= 0) return el;
      }
    }
    return null;
  }

  function fireMouse(el, name, x, y){
    try {
      var ev = new MouseEvent(name, {bubbles: true, cancelable: true, clientX: x, clientY: y});
      el.dispatchEvent(ev);
    } catch (e) { }
  }

  function fireClick(el){
    if (!el) return;
    var r = el.getBoundingClientRect();
    var x = r.left + r.width / 2, y = r.top + r.height / 2;
    if (r.width === 0 && r.height === 0) { x = window.innerWidth / 2; y = window.innerHeight / 2; }
    fireMouse(el, 'mousedown', x, y);
    fireMouse(el, 'mouseup', x, y);
    fireMouse(el, 'click', x, y);
    try { el.click(); } catch (e) { }
  }

  function fireKey(dir){
    var key = dir < 0 ? 'ArrowLeft' : 'ArrowRight';
    var code = dir < 0 ? 37 : 39;
    ['keydown','keyup'].forEach(function(t){
      var opts = {key: key, code: key, keyCode: code, which: code, bubbles: true, cancelable: true};
      try {
        var ev = new KeyboardEvent(t, opts);
        document.dispatchEvent(ev);
        window.dispatchEvent(ev);
      } catch (e) { }
    });
  }

  function fireEdgeClick(dir){
    var x = dir < 0 ? window.innerWidth * 0.12 : window.innerWidth * 0.88;
    var y = window.innerHeight / 2;
    var el = document.elementFromPoint(x, y);
    if (!el) el = document.body;
    fireMouse(el, 'mousedown', x, y);
    fireMouse(el, 'mouseup', x, y);
    fireMouse(el, 'click', x, y);
  }

  function firePointer(el, name, x, y){
    try {
      var ev = new PointerEvent(name, {bubbles: true, cancelable: true, pointerId: 7, pointerType: 'touch', isPrimary: true, clientX: x, clientY: y});
      el.dispatchEvent(ev);
    } catch (e) { }
  }

  function fireTouch(el, name, cx, cy){
    if (!el) el = document.body;
    try {
      var t = new Touch({identifier: 7, target: el, clientX: cx, clientY: cy, pageX: cx, pageY: cy, screenX: cx, screenY: cy});
      var tl = name === 'touchend' ? [] : [t];
      var ev = new TouchEvent(name, {bubbles: true, cancelable: true, touches: tl, targetTouches: tl, changedTouches: [t]});
      el.dispatchEvent(ev);
    } catch (e) {
      var plain = [{clientX: cx, clientY: cy}];
      var ev2 = null;
      try { ev2 = new window.Event(name, {bubbles: true, cancelable: true}); } catch (e2) {
        ev2 = document.createEvent('Event');
        ev2.initEvent(name, true, true);
      }
      try { Object.defineProperty(ev2, 'touches', {value: name === 'touchend' ? [] : plain}); } catch (e3) { }
      try { Object.defineProperty(ev2, 'changedTouches', {value: plain}); } catch (e4) { }
      el.dispatchEvent(ev2);
    }
  }

  function fireSwipe(el, dir){
    var w = window.innerWidth, h = window.innerHeight;
    var off = Math.min(w, h) * 0.30;
    var cx = w / 2, cy = h / 2;
    var sx = cx + dir * off, ex = cx - dir * off;
    var targets = [];
    if (el && el !== document.body) targets.push(el);
    var centerEl = document.elementFromPoint(cx, cy);
    if (centerEl) targets.push(centerEl);
    targets.push(document.body);

    targets.forEach(function(t){
      if (!t) return;
      fireTouch(t, 'touchstart', sx, cy);
      firePointer(t, 'pointerdown', sx, cy);
    });

    var steps = 0;
    var timer = setInterval(function(){
      steps++;
      var tx = sx + (ex - sx) * (steps / 5);
      targets.forEach(function(t){
        if (!t) return;
        fireTouch(t, 'touchmove', tx, cy);
        firePointer(t, 'pointermove', tx, cy);
      });
      if (steps >= 5) {
        clearInterval(timer);
        targets.forEach(function(t){
          if (!t) return;
          fireTouch(t, 'touchend', ex, cy);
          firePointer(t, 'pointerup', ex, cy);
        });
      }
    }, 25);
  }

  window.__tvSwipe = function(dir){
    var ov = findOverlay();
    var arrow = ov ? findArrow(ov, dir) : null;
    if (arrow) { fireClick(arrow); return true; }
    if (ov) {
      fireSwipe(ov, dir);
      fireEdgeClick(dir);
      return true;
    }
    if (!('ontouchstart' in window)) { fireKey(dir); }
    return false;
  };

  window.__tvClick = function(){
    var el = document.activeElement;
    if (el && el.tagName !== 'BODY' && el.tagName !== 'HTML') {
      fireClick(el);
      return true;
    }
    return false;
  };

  window.__tvEscape = function(){
    var opts = {key: 'Escape', code: 'Escape', keyCode: 27, which: 27, bubbles: true, cancelable: true};
    ['keydown','keyup'].forEach(function(t){
      try {
        var e = new KeyboardEvent(t, opts);
        document.dispatchEvent(e);
        window.dispatchEvent(e);
      } catch (err) { }
    });
    return true;
  };

  return true;
})();