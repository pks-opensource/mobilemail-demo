package com.vaadin.demo.mobilemail.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.vaadin.addon.touchkit.ui.NavigationBar;
import com.vaadin.addon.touchkit.ui.NavigationButton;
import com.vaadin.addon.touchkit.ui.NavigationButton.NavigationButtonClickEvent;
import com.vaadin.addon.touchkit.ui.NavigationView;
import com.vaadin.addon.touchkit.ui.VerticalComponentGroup;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.demo.mobilemail.MobileMailUI;
import com.vaadin.demo.mobilemail.data.AbstractPojo;
import com.vaadin.demo.mobilemail.data.DummyDataUtil;
import com.vaadin.demo.mobilemail.data.Folder;
import com.vaadin.demo.mobilemail.data.MailBox;
import com.vaadin.demo.mobilemail.data.Message;
import com.vaadin.demo.mobilemail.data.MessageStatus;
import com.vaadin.demo.mobilemail.data.MobileMailContainer;
import com.vaadin.demo.mobilemail.data.ParentFilter;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIDetachedException;

/**
 * Displays accounts, mailboxes, message list hierarchically
 */
@SuppressWarnings("serial")
public class MailboxHierarchyView extends NavigationView {

    private final Map<MailBox, NavigationButton> mailBoxes = Maps.newHashMap();

    private final Resource mailboxIcon = FontAwesome.GLOBE;

    static Resource reloadIcon = FontAwesome.REFRESH;

    private static Button reload;

    private static Map<UI, Folder> vmailInboxes = Maps.newConcurrentMap();

    static {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                for (final Entry<UI, Folder> entry : new HashSet<Entry<UI, Folder>>(
                        vmailInboxes.entrySet())) {
                    try {
                        entry.getKey().access(new Runnable() {
                            @Override
                            public void run() {
                                MobileMailContainer container = (MobileMailContainer) entry
                                        .getKey().getData();
                                Folder vmailInbox = entry.getValue();
                                Message newMessage = DummyDataUtil
                                        .createMessage(vmailInbox,
                                                MessageStatus.NEW);
                                vmailInbox.getChildren().remove(newMessage);
                                vmailInbox.getChildren().add(0, newMessage);

                                container.addItemAt(0, newMessage);
                            }
                        });
                    } catch (final UIDetachedException e) {
                        // Ignore
                    } catch (final NullPointerException e) {
                        // Ignore
                    }
                }
            }
        }, new Date(), 10000);
    }

    public MailboxHierarchyView(final MobileMailContainer ds, final MailboxHierarchyManager nav) {
        setCaption("Mailboxes");
        setWidth("100%");
        setHeight("100%");

        // Mailboxes do not have parents
        ds.setFilter(new ParentFilter(null));

        CssLayout root = new CssLayout();

        VerticalComponentGroup accounts = new VerticalComponentGroup();
        Label header = new Label("Accounts");
        header.setSizeUndefined();
        header.addStyleName("grey-title");
        root.addComponent(header);

        for (AbstractPojo itemId : ds.getItemIds()) {
            final MailBox mb = (MailBox) itemId;
            NavigationButton btn = new NavigationButton(mb.getName());
            if (mb.getName().length() > 20) {
                btn.setCaption(mb.getName().substring(0, 20) + "…");
            }
            btn.setIcon(mailboxIcon);

            btn.addClickListener(new NavigationButton.NavigationButtonClickListener() {

                private static final long serialVersionUID = 1L;

                @Override
                public void buttonClick(NavigationButtonClickEvent event) {
                    FolderHierarchyView v = new FolderHierarchyView(nav, ds, mb);
                    nav.navigateTo(v);
                }
            });

            btn.addStyleName("pill");
            accounts.addComponent(btn);

            mailBoxes.put(mb, btn);
        }

        root.addComponent(accounts);
        setContent(root);
        setToolbar(createToolbar());

        final UI ui = UI.getCurrent();
        ui.setData(ds);
        ds.addItemSetChangeListener(new ItemSetChangeListener() {
            @Override
            public void containerItemSetChange(ItemSetChangeEvent event) {
                 updateNewMessages();
            }
        });
        updateNewMessages();

        MailBox vmail = (MailBox) ds.getIdByIndex(0);
        Folder vmailInbox = (Folder) ds.getChildren(vmail).iterator().next();

        vmailInboxes.put(ui, vmailInbox);

        UI.getCurrent().addDetachListener(new DetachListener() {
            @Override
            public void detach(DetachEvent event) {
                vmailInboxes.remove(ui);
            }
        });
    }

    static Component createToolbar() {

        final NavigationBar toolbar = new NavigationBar();

        reload = new Button();
        reload.setIcon(reloadIcon);
        reload.addStyleName("reload");
        reload.addStyleName("no-decoration");

        toolbar.setLeftComponent(reload);

        final SimpleDateFormat formatter = new SimpleDateFormat("M/d/yy hh:mm");
        toolbar.setCaption("Updated "
                + formatter.format(Calendar.getInstance().getTime()));

        reload.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                toolbar.setCaption("Updated "
                        + formatter.format(Calendar.getInstance().getTime()));
            }
        });

        UI touchKitApplication = MobileMailUI.getCurrent();
        if (touchKitApplication instanceof MobileMailUI) {
            MobileMailUI app = (MobileMailUI) touchKitApplication;
            if (app.isSmallScreenDevice()) {
                /*
                 * For small screen devices we add shortcut to new message below
                 * hierarcy views
                 */
                ClickListener showComposeview = new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        ComposeView cv = new ComposeView(true);
                        cv.showRelativeTo(event.getButton());
                    }
                };
                Button button = new Button(null, showComposeview);
                button.addStyleName("compose");
                button.setIcon(FontAwesome.PENCIL_SQUARE_O);
                toolbar.setRightComponent(button);
                button.addStyleName("no-decoration");
            }
        }

        return toolbar;
    }

    private void updateNewMessages() {
        for (Entry<MailBox, NavigationButton> entry : mailBoxes.entrySet()) {
            // Set new messages
            int unreadMessages = 0;
            for (Folder child : entry.getKey().getFolders()) {
                for (AbstractPojo p : child.getChildren()) {
                    if (p instanceof Message) {
                        Message msg = (Message) p;
                        unreadMessages += msg.getStatus() != MessageStatus.READ ? 1
                                : 0;
                    }
                }
            }
            if (unreadMessages > 0) {
                entry.getValue().setDescription(unreadMessages + "");
            } else {
                entry.getValue().setDescription(null);
            }
        }
    }
}
