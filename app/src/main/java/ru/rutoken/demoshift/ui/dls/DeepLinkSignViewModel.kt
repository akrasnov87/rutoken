package ru.rutoken.demoshift.ui.dls

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.rutoken.demoshift.repository.User
import ru.rutoken.demoshift.repository.UserRepository
import ru.rutoken.demoshift.ui.document.DocumentViewModel
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DeepLinkSignViewModel(context: Context, private val repository: UserRepository) : ViewModel() {
    @SuppressLint("StaticFieldLeak")
    private var mContext: Context

    init {
        mContext = context
    }
    fun getUsers(): LiveData<List<User>> = repository.getUsersAsync()

    val documentUri: MutableLiveData<Uri> = MutableLiveData(
        FileProvider.getUriForFile(
            context,
            "ru.rutoken.demoshift.fileprovider",
            File(context.cacheDir, "/document.pdf")
        )
    )

    fun getDefaultUserId(): Int? {
        return UserUtils.readDefaultUser(mContext)
    }
}