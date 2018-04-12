/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package co.chatsdk.ui.threads;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.events.EventType;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.interfaces.ThreadType;
import co.chatsdk.core.session.NM;
import co.chatsdk.ui.Survey.surveyActivity;
import co.chatsdk.ui.helpers.DialogUtils;
import co.chatsdk.ui.login.LoginActivity;
import co.chatsdk.ui.manager.InterfaceManager;
import co.chatsdk.ui.R;
import co.chatsdk.ui.main.BaseFragment;
import co.chatsdk.ui.utils.ToastHelper;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;

/**
 * Created by itzik on 6/17/2014.
 */
public class PublicThreadsFragment extends BaseFragment {

    private RecyclerView listThreads;
    private ThreadsListAdapter adapter;
    private int clearedThreads = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initViews(inflater);

        NM.events().sourceOnMain()
                .filter(NetworkEvent.filterPublicThreadsUpdated())
                .subscribe(networkEvent -> {
                    if(tabIsVisible) {
                        reloadData();
                    }

                });

        NM.events().sourceOnMain()
                .filter(NetworkEvent.filterType(EventType.TypingStateChanged))
                .subscribe(networkEvent -> {
                    if(tabIsVisible) {
                        adapter.setTyping(networkEvent.thread, networkEvent.text);
                        adapter.notifyDataSetChanged();
                    }
                });

        reloadData();

        return mainView;
    }

    public void initViews(LayoutInflater inflater) {
        mainView = inflater.inflate(R.layout.chat_sdk_activity_threads, null);
        listThreads = (RecyclerView) mainView.findViewById(R.id.list_threads);
        adapter = new ThreadsListAdapter(getActivity());

        listThreads.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        listThreads.setAdapter(adapter);

        adapter.onClickObservable().subscribe(thread -> InterfaceManager.shared().a.startChatActivityForID(getContext(), thread.getEntityID()));


        if (LoginActivity.isProfessional ==1) {


            adapter.onLongClickObservable().subscribe(thread -> ToastHelper.show(getContext(), "Student risk: "+ (surveyActivity.riskScore)));



        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item =
                menu.add(Menu.NONE, R.id.action_chat_sdk_add, 10, getString(R.string.public_thread_fragment_add_item_text));
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setIcon(R.drawable.ic_plus);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Cant use switch in the library*/
        int id = item.getItemId();


        if (id == R.id.action_chat_sdk_add)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
            builder.setTitle("Please Enter Display Name:");

            // Set up the input
            final EditText input = new EditText(this.getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton(getString(R.string.create), (dialog, which) -> {

                showOrUpdateProgressDialog(getString(R.string.add_public_chat_dialog_progress_message));
                final String threadName = input.getText().toString();

                NM.publicThread().createPublicThreadWithName(threadName)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((thread, throwable) -> {
                            if(throwable == null) {
                                dismissProgressDialog();
                                //listThreads = (RecyclerView) mainView.findViewById(R.id.list_threads);
                                //adapter = new ThreadsListAdapter(getActivity());
                                adapter.addRow(thread);

                                // TODO: Improve this
                                ToastHelper.show(getContext(), getString(R.string.add_public_chat_dialog_toast_success_before_thread_name) + threadName + getString(R.string.add_public_chat_dialog_toast_success_after_thread_name) );

                                clearedThreads=0;
                                InterfaceManager.shared().a.startChatActivityForID(getContext(), thread.getEntityID());
                            }
                            else {
                                throwable.printStackTrace();
                                Toast.makeText(PublicThreadsFragment.this.getContext(), throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                dismissProgressDialog();                            }
                        });

            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

            builder.show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void clearData() {
        if(adapter != null) {
            adapter.clearData();
        }
    }

    public void setTabVisibility (boolean isVisible) {
        super.setTabVisibility(isVisible);
        reloadData();
    }

    @Override
    public void reloadData() {
        if(adapter != null) {
            adapter.updateThreads(NM.thread().getThreads(ThreadType.Public));
        }
        if (clearedThreads == 1){
            clearData();
        }
    }

}