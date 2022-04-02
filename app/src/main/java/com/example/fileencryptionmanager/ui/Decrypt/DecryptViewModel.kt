package com.example.fileencryptionmanager.ui.Decrypt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DecryptViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is decrypt Fragment"
    }
    val text: LiveData<String> = _text
}