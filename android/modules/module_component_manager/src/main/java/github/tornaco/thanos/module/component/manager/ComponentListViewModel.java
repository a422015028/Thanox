package github.tornaco.thanos.module.component.manager;

import android.app.Application;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import github.tornaco.android.thanos.core.app.ThanosManager;
import github.tornaco.android.thanos.core.pm.AppInfo;
import github.tornaco.android.thanos.core.util.Rxs;
import github.tornaco.thanos.module.component.manager.model.ComponentModel;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rx2.android.schedulers.AndroidSchedulers;
import util.CollectionUtils;
import util.Consumer;

public abstract class ComponentListViewModel extends AndroidViewModel {

    private final ObservableBoolean isDataLoading = new ObservableBoolean(false);
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final ObservableArrayList<ComponentModel> componentModels = new ObservableArrayList<>();

    private AppInfo appInfo;

    private final ObservableField<String> queryText = new ObservableField<>(null);

    ComponentListViewModel(@NonNull Application application) {
        super(application);
        registerEventReceivers();
    }

    public void setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    void start() {
        loadComponents();
    }

    private void loadComponents() {
        if (appInfo == null) {
            isDataLoading.set(false);
            return;
        }
        if (!ThanosManager.from(getApplication()).isServiceInstalled()) return;
        if (isDataLoading.get()) return;
        isDataLoading.set(true);
        componentModels.clear();
        disposables.add(Single
                .create((SingleOnSubscribe<List<ComponentModel>>) emitter ->
                        emitter.onSuccess(Objects.requireNonNull(onCreateLoader().load(appInfo))))
                .flatMapObservable((Function<List<ComponentModel>,
                        ObservableSource<ComponentModel>>) Observable::fromIterable)
                .filter(componentModel -> {
                    String query = queryText.get();
                    return TextUtils.isEmpty(query)
                            || componentModel.getName().toLowerCase(Locale.US).contains(query.toLowerCase(Locale.US));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> componentModels.clear())
                .subscribe(componentModels::add, Rxs.ON_ERROR_LOGGING, () -> isDataLoading.set(false)));
    }

    protected abstract ComponentsLoader onCreateLoader();

    public void toggleComponentState(@Nullable ComponentModel componentModel, boolean checked) {
        if (componentModel == null) return;
        ThanosManager thanox = ThanosManager.from(getApplication());
        if (thanox.isServiceInstalled()) {
            thanox.getActivityManager().forceStopPackage(componentModel.getComponentName().getPackageName());
            // Wait a moment.
            disposables.add(Completable.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    // Empty.
                }
            }).delay(500, TimeUnit.MILLISECONDS)
                    .observeOn(Schedulers.io())
                    .subscribe(new Action() {
                        @Override
                        public void run() {
                            thanox.getPkgManager().setComponentEnabledSetting(
                                    componentModel.getComponentName(),
                                    checked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0 /* Kill it */);
                        }
                    }));

        }
    }

    private void registerEventReceivers() {
    }

    private void unRegisterEventReceivers() {
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        unRegisterEventReceivers();
    }

    public void setSearchText(String newText) {
        if (TextUtils.isEmpty(newText)) return;
        queryText.set(newText);
        loadComponents();
    }

    public void clearSearchText() {
        queryText.set(null);
        loadComponents();
    }

    public void selectAll(boolean enabled) {
        CollectionUtils.consumeRemaining(componentModels, new Consumer<ComponentModel>() {
            @Override
            public void accept(ComponentModel componentModel) {
                toggleComponentState(componentModel, enabled);
            }
        });
        // Wait 1s.
        disposables.add(Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                // Noop.
            }
        }).delay(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::start));
    }

    public ObservableBoolean getIsDataLoading() {
        return this.isDataLoading;
    }

    public ObservableArrayList<ComponentModel> getComponentModels() {
        return this.componentModels;
    }

    public interface ComponentsLoader {
        @WorkerThread
        List<ComponentModel> load(AppInfo appInfo);
    }
}
