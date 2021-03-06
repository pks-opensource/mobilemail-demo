package com.vaadin.demo.mobilemail.ui;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import com.vaadin.addon.touchkit.ui.HorizontalButtonGroup;
import com.vaadin.addon.touchkit.ui.NavigationButton;
import com.vaadin.addon.touchkit.ui.NavigationView;
import com.vaadin.addon.touchkit.ui.Popover;
import com.vaadin.addon.touchkit.ui.Toolbar;
import com.vaadin.demo.mobilemail.data.AbstractPojo;
import com.vaadin.demo.mobilemail.data.Folder;
import com.vaadin.demo.mobilemail.data.Message;
import com.vaadin.demo.mobilemail.data.MessageField;
import com.vaadin.demo.mobilemail.data.MessageStatus;
import com.vaadin.demo.mobilemail.data.MobileMailContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;
import com.vaadin.ui.themes.Reindeer;

/**
 * A navigation view to display a single message.
 * 
 */
@SuppressWarnings("serial")
public class MessageView extends NavigationView implements ClickListener {

    private final CssLayout layout = new CssLayout();
    private final CssLayout detailsLayout = new CssLayout();

    private final HorizontalButtonGroup navigationActions = new HorizontalButtonGroup();
    private Button nextButton;
    private Button prevButton;
    private Button markAsUnreadButton;

    private final Toolbar messageActions = new Toolbar();
    private Button moveButton;
    private Button composeButton;
    private Button deleteButton;
    private Button replyButton;
    private Popover replyOptions;
    private Button replyOptionsReply;
    private Button replyOptionsReplyAll;
    private Button replyOptionsForward;
    private Button replyOptionsPrint;
    private final VerticalLayout replyOptionsLayout = new VerticalLayout();
    private final boolean smartphone;
    private final MobileMailContainer ds;

    public MessageView(MobileMailContainer ds, boolean smartphone) {
        this.ds = ds;
        this.smartphone = smartphone;
        setContent(layout);
        layout.setWidth("100%");
        layout.setStyleName("message-layout");
        addStyleName("message-view");

        buildToolbar();

        if (smartphone) {
            setToolbar(messageActions);
            setRightComponent(navigationActions);
        } else {
            messageActions.setStyleName(null);
            messageActions.setWidth("200px");
            messageActions.setHeight("32px");
            setRightComponent(messageActions);
            setLeftComponent(navigationActions);
        }

        setMessage(null, null);
    }

    private void buildToolbar() {
        moveButton = new Button(null, this);
        composeButton = new Button(null, this);
        deleteButton = new Button(null, this);
        replyButton = new Button(null, new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                Popover pop = new Popover();
                VerticalLayout content = new VerticalLayout();
                content.setMargin(true);
                content.setSpacing(true);
                pop.setContent(content);
                pop.setWidth("300px");
                Button reply = new Button("Reply", MessageView.this);
                reply.addStyleName("reply");
                reply.setWidth("100%");
                Button replyAll = new Button("Reply All", MessageView.this);
                replyAll.addStyleName("white");
                replyAll.setWidth("100%");
                Button forward = new Button("Forward", MessageView.this);
                forward.addStyleName("white");
                forward.setWidth("100%");
                Button print = new Button("Print", MessageView.this);
                print.addStyleName("white");
                print.setWidth("100%");
                pop.addComponent(reply);
                pop.addComponent(replyAll);
                pop.addComponent(forward);
                pop.addComponent(print);
                pop.showRelativeTo(event.getButton());
            }
        });

        moveButton.setStyleName("no-decoration");
        moveButton.setIcon(FontAwesome.FOLDER_O);
        composeButton.setStyleName("no-decoration");
        composeButton.setIcon(FontAwesome.PENCIL);
        deleteButton.setStyleName("no-decoration");
        deleteButton.setIcon(FontAwesome.TRASH_O);
        replyButton.setStyleName("no-decoration");
        replyButton.setIcon(FontAwesome.MAIL_REPLY);

        messageActions.addComponent(moveButton);
        messageActions.addComponent(deleteButton);
        messageActions.addComponent(replyButton);
        messageActions.addComponent(composeButton);

        nextButton = new Button("Down", this);
        nextButton.addStyleName("icon-arrow-down");
        nextButton.setEnabled(false);
        // nextButton.setVisible(smartphone);

        prevButton = new Button("Up", this);
        prevButton.addStyleName("icon-arrow-up");
        prevButton.setEnabled(false);
        // prevButton.setVisible(smartphone);

        navigationActions.addComponent(prevButton);
        navigationActions.addComponent(nextButton);

    }

    /**
     * @return the layout that contains e.g prev and next buttons. Note that
     *         this component is not attached by defaults. Users of this class
     *         can assign it where appropriate (tablet and smartphone views want
     *         to locate this differently).
     */
    public HorizontalButtonGroup getNavigationLayout() {
        return navigationActions;
    }

    public Button getNavigationPrevButton() {
        return prevButton;
    }

    public Button getNavigationNextButton() {
        return nextButton;
    }

    private Message message;
    private MessageHierarchyView currentMessageList;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message msg, MessageHierarchyView messageList) {
        currentMessageList = messageList;

        if (msg != null) {
            message = msg;

            layout.removeAllComponents();
            detailsLayout.removeAllComponents();
            detailsLayout.setStyleName("message-details");

            Label lbl = new Label("From: ");
            lbl.setStyleName("light-text");
            lbl.setSizeUndefined();
            layout.addComponent(lbl);

            MessageField from = message.getMessageField("from");
            NativeButton fromField = new NativeButton(from.getValue(), this);
            fromField.addStyleName("from-button");
            layout.addComponent(fromField);

            Button button = new Button("Details");
            button.setStyleName(BaseTheme.BUTTON_LINK);
            button.addStyleName("details-link");
            button.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    detailsLayout.setVisible(!detailsLayout.isVisible());

                    if (detailsLayout.isVisible()) {
                        event.getButton().setCaption("Hide");
                    } else {
                        event.getButton().setCaption("Details");
                    }

                    if (markAsUnreadButton != null && message.getStatus() == MessageStatus.READ) {
                        markAsUnreadButton.setVisible(detailsLayout.isVisible());
                    }
                }
            });
            layout.addComponent(button);

            lbl = new Label("<hr/>", ContentMode.HTML);
            layout.addComponent(lbl);

            detailsLayout.setVisible(false);
            layout.addComponent(detailsLayout);

            List<MessageField> fields = message.getFields();
            for (MessageField f : fields) {
                if (f.getCaption().equals("From")
                        || f.getCaption().equals("Subject")
                        || f.getCaption().equals("Body")) {
                    continue;
                }

                lbl = new Label(f.getCaption() + ": ");
                lbl.setStyleName("light-text");
                lbl.setSizeUndefined();
                detailsLayout.addComponent(lbl);

                Button btn = new NativeButton(f.getValue(), this);
                btn.addStyleName("from-button");
                detailsLayout.addComponent(btn);

                lbl = new Label("<hr/>", ContentMode.HTML);
                detailsLayout.addComponent(lbl);
            }

            MessageField subject = message.getMessageField("subject");
            CssLayout subjectField = new CssLayout();
            subjectField.setWidth("100%");

            lbl = new Label(subject.getValue());
            lbl.setStyleName(Reindeer.LABEL_H2);
            subjectField.addComponent(lbl);

            SimpleDateFormat formatter = new SimpleDateFormat(
                    "M/d/yy '<b>'hh:mm'</b>'");

            lbl = new Label(formatter.format(message.getTimestamp()),
                    ContentMode.HTML);
            lbl.setStyleName(Reindeer.LABEL_SMALL);
            lbl.setSizeUndefined();
            subjectField.addComponent(lbl);

            if (message.getStatus() == MessageStatus.READ) {
                markAsUnreadButton = new Button("Mark as Unread", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        markAsUnreadButton.setVisible(false);
                        message.setStatus(MessageStatus.UNREAD);
                        ds.refresh();
                    }
                });
                markAsUnreadButton.setVisible(false);
                markAsUnreadButton.setStyleName("mark-as-unread-button");
                markAsUnreadButton.addStyleName(BaseTheme.BUTTON_LINK);
                markAsUnreadButton.setIcon(FontAwesome.BELL_O);
                subjectField.addComponent(markAsUnreadButton);
            }

            layout.addComponent(subjectField);

            lbl = new Label("<hr/>", ContentMode.HTML);
            layout.addComponent(lbl);

            MessageField body = message.getMessageField("body");
            Label label = new Label(body.getValue(), ContentMode.HTML);
            layout.addComponent(label);
            removeStyleName("no-message");

        } else {
            layout.removeAllComponents();
            Label noMessageLbl = new Label("No Message Selected");
            noMessageLbl.setStyleName(Reindeer.LABEL_SMALL);
            noMessageLbl.addStyleName(Reindeer.LABEL_H1);
            layout.addComponent(noMessageLbl);
            addStyleName("no-message");
            nextButton.setEnabled(false);
            prevButton.setEnabled(false);
        }

        updateNewMessages();
    }

    @Override
    public void buttonClick(ClickEvent event) {
        if (event.getButton() == replyButton) {
            showReplyButtonOptions();
            return;
        }
        if (event.getButton() == composeButton) {
            ComposeView composeView = new ComposeView(smartphone);
            getUI().addWindow(composeView);
            composeView.bringToFront();
            // composeView.showRelativeTo(event.getButton());
            return;
        }
        if (event.getButton() == nextButton) {
            Folder folder = (Folder) message.getParent();
            List<AbstractPojo> messagesAndFolders = folder.getChildren();
            int index = messagesAndFolders.indexOf(message);
            if (index < messagesAndFolders.size() - 1) {
                Message msg = (Message) messagesAndFolders.get(index + 1);
                currentMessageList.selectMessage(msg);
                setMessage(msg, currentMessageList);
            }
            return;

        }
        if (event.getButton() == prevButton) {
            Folder folder = (Folder) message.getParent();
            List<AbstractPojo> messagesAndFolders = folder.getChildren();
            int index = messagesAndFolders.indexOf(message);
            if (index > 0) {
                Message msg = (Message) messagesAndFolders.get(index - 1);
                currentMessageList.selectMessage(msg);
                setMessage(msg, currentMessageList);
            }
            return;
        }

        if (event.getButton().getParent() == replyOptionsLayout) {
            replyOptions.getUI().removeWindow(replyOptions);
        }

        Notification.show("Not implemented");

    }

    private void showReplyButtonOptions() {
        if (replyOptions == null) {
            replyOptions = new Popover();
            replyOptionsLayout.setMargin(true);
            replyOptionsLayout.setSpacing(true);
            replyOptions.setWidth("300px");
            replyOptions.setClosable(true);
            replyOptions.setContent(replyOptionsLayout);
            replyOptionsLayout.setSpacing(true);

            replyOptionsReply = new Button("Reply", this);
            replyOptionsReply.setWidth("100%");
            replyOptionsReply.addStyleName("reply");
            replyOptionsReplyAll = new Button("Reply all", this);
            replyOptionsReplyAll.setWidth("100%");
            replyOptionsForward = new Button("Forward", this);
            replyOptionsForward.setWidth("100%");
            replyOptionsPrint = new Button("Print", this);
            replyOptionsPrint.setWidth("100%");

            replyOptions.addComponent(replyOptionsReply);
            replyOptions.addComponent(replyOptionsReplyAll);
            replyOptions.addComponent(replyOptionsForward);
            replyOptions.addComponent(replyOptionsPrint);

        }

        replyOptions.showRelativeTo(replyButton);
    }

    public void updateNewMessages() {
        String caption = null;
        if (message != null) {
            Folder folder = (Folder) message.getParent();
            List<AbstractPojo> siblings = folder.getChildren();
            int index = siblings.indexOf(message);

            nextButton.setEnabled(true);
            prevButton.setEnabled(true);
            if (index == 0) {
                prevButton.setEnabled(false);
            }
            if (index == siblings.size() - 1) {
                nextButton.setEnabled(false);
            }

            caption = (index + 1) + " of " + siblings.size();
        }
        setCaption(caption);

        NavigationButton backButton = getBackButton();
        if (backButton != null) {
            Component target = backButton.getTargetView();
            if (target != null && target == getPreviousComponent()) {
                backButton.setCaption(getPreviousComponent().getCaption());
            }
        }
    }

    private NavigationButton getBackButton() {
        NavigationButton backButton = null;
        Iterator<Component> i = getNavigationBar().getComponentIterator();
        while (i.hasNext()) {
            Component comp = i.next();
            if (comp instanceof NavigationButton) {
                backButton = (NavigationButton) comp;
            }
        }
        return backButton;
    }
}
