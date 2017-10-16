"use strict";
function getScrollHeight() {
    return document.scrollingElement.scrollHeight;
}

function writeScrollHeight() {
    document.write(getScrollHeight());
}
