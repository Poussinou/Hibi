package app.marcdev.hibi.internal.base

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class ScopedDialogFragment : DialogFragment(), CoroutineScope {
  private lateinit var job: Job

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    job = Job()
  }

  override fun onDestroy() {
    super.onDestroy()
    job.cancel()
  }
}