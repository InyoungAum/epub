"use strict";
function getScrollHeight() {
    return document.documentElement.scrollHeight
}

function getScreenHeight() {
    return window.innerHeight
}

function getCurrentScrollHeight() {
    return document.documentElement.scrollTop
}

function calcPageCount() {
    return Math.round((getScrollHeight() - getBodyPaddingTop() - getBodyPaddingBottom()) /
                        getScreenHeight() - 0.5)
}

function scrollAbsY(y) {
    window.scrollTo(0, y)
}

function getBodyPaddingTop() {
    var body = document.getElementsByTagName('body')[0];
    return parseInt(window.getComputedStyle(body, null).getPropertyValue('padding-top'))
}

function getBodyPaddingBottom() {
    var body = document.getElementsByTagName('body')[0];
    return parseInt(window.getComputedStyle(body, null).getPropertyValue('padding-bottom'))
}

function scrollByOffset(offset) {
    var paddingTop = getBodyPaddingTop()
    var paddingBottom = getBodyPaddingBottom()
    var amount = 0

    if (offset == 0) {
        amount = parseInt(paddingTop, 10)
    } else if (offset == calcPageCount()) {
        amount = getScrollHeight() - getScreenHeight() - paddingBottom
    } else {
        amount = offset * getScreenHeight() + paddingTop
    }

    window.scrollTo(0, amount)
}

function scrollToAnchor(anchor) {
    var top = document.getElementById(anchor).offsetTop;
    window.scrollTo(0, top);
}
