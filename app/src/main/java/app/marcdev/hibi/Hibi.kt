package app.marcdev.hibi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import app.marcdev.hibi.data.BackupUtils
import app.marcdev.hibi.data.database.AppDatabase
import app.marcdev.hibi.data.database.DAO
import app.marcdev.hibi.data.database.ProductionAppDatabase
import app.marcdev.hibi.data.network.ConnectivityInterceptor
import app.marcdev.hibi.data.network.ConnectivityInterceptorImpl
import app.marcdev.hibi.data.network.JishoAPIService
import app.marcdev.hibi.data.repository.*
import app.marcdev.hibi.entryscreens.addentryscreen.AddEntryViewModelFactory
import app.marcdev.hibi.entryscreens.viewentryscreen.ViewEntryViewModelFactory
import app.marcdev.hibi.internal.NOTIFICATION_CHANNEL_REMINDER_ID
import app.marcdev.hibi.maintabs.calendarfragment.CalendarTabViewModelFactory
import app.marcdev.hibi.maintabs.mainentries.MainEntriesViewModelFactory
import app.marcdev.hibi.search.searchmoreinfoscreen.SearchMoreInfoViewModelFactory
import app.marcdev.hibi.search.searchresults.SearchViewModelFactory
import app.marcdev.hibi.uicomponents.addnewworddialog.AddNewWordViewModelFactory
import app.marcdev.hibi.uicomponents.addtagdialog.AddTagViewModelFactory
import app.marcdev.hibi.uicomponents.addtagtoentrydialog.AddTagToEntryViewModelFactory
import app.marcdev.hibi.uicomponents.newwordsdialog.NewWordViewModelFactory
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import timber.log.Timber

class Hibi : Application(), KodeinAware {

  override val kodein = Kodein.lazy {
    import(androidXModule(this@Hibi))

    bind<AppDatabase>() with singleton { ProductionAppDatabase.invoke(applicationContext) }
    bind<DAO>() with singleton { instance<AppDatabase>().dao() }
    bind<EntryRepository>() with singleton { EntryRepositoryImpl.getInstance(instance()) }
    bind<TagRepository>() with singleton { TagRepositoryImpl.getInstance(instance()) }
    bind<TagEntryRelationRepository>() with singleton { TagEntryRelationRepositoryImpl.getInstance(instance()) }
    bind<NewWordRepository>() with singleton { NewWordRepositoryImpl.getInstance(instance()) }
    bind<ConnectivityInterceptor>() with singleton { ConnectivityInterceptorImpl(instance()) }
    bind<JishoAPIService>() with singleton { JishoAPIService(instance()) }
    bind() from provider { MainEntriesViewModelFactory(instance(), instance()) }
    bind() from provider { AddEntryViewModelFactory(instance(), instance(), instance()) }
    bind() from provider { ViewEntryViewModelFactory(instance(), instance(), instance()) }
    bind() from provider { SearchViewModelFactory(instance()) }
    bind() from provider { SearchMoreInfoViewModelFactory() }
    bind() from provider { AddTagToEntryViewModelFactory(instance(), instance()) }
    bind() from provider { AddTagViewModelFactory(instance()) }
    bind() from provider { NewWordViewModelFactory(instance()) }
    bind() from provider { AddNewWordViewModelFactory(instance()) }
    bind() from provider { CalendarTabViewModelFactory(instance(), instance()) }
    bind() from provider { BackupUtils(instance()) }
  }

  override fun onCreate() {
    super.onCreate()
    if(BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      Timber.i("Log: Timber Debug Tree planted")
    }
    createNotificationChannels()
  }

  private fun createNotificationChannels() {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val reminderChannel = NotificationChannel(
        NOTIFICATION_CHANNEL_REMINDER_ID,
        resources.getString(R.string.reminder_notification_channel_title),
        NotificationManager.IMPORTANCE_HIGH)
      reminderChannel.description = resources.getString(R.string.reminder_notification_channel_description)

      val manager: NotificationManager = getSystemService(NotificationManager::class.java)
      manager.createNotificationChannel(reminderChannel)
    }
  }
}