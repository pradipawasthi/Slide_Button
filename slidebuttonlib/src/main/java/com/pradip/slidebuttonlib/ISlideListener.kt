package com.pradip.slidebuttonlib

interface ISlideListener {
    fun onSlideStart()
    fun onSlideMove(percent: Float)
    fun onSlideCancel()
    fun onSlideDone()
}
