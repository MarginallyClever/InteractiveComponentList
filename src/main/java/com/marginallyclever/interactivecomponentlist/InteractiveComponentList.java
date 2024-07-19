package com.marginallyclever.interactivecomponentlist;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>{@link InteractiveComponentList} contains {@link Component}s that can be vertically rearranged by dragging a handle.</p>
 * <p>To use, call <code>add(component)</code> on {@link InteractiveComponentList}, which will wrap <code>component</code> in a
 * {@link InteractiveComponentListMiddle} and add it to the bottom end of the panel.
 * <p>{@link InteractiveComponentList} fires {@link ListDataEvent} to all {@link ListDataListener} subscribers when the
 * order of the list is changed.</p>
 * <p>The {@link InteractiveComponentList} also supports auto-scrolling when dragging a elements near the top or bottom of the viewport.</p>
 * <p>Because the {@link Component}s are wrapped, calling <code>getComponent</code> will return the {@link InteractiveComponentListMiddle}.
 * <p>To obtain the item being wrapped, use <code>getInnerComponent(int index)</code>.  Calling remove() with the results of either
 * <code>getComponent</code> or <code>getInnerComponent</code> will work.</p>
 */
public class InteractiveComponentList extends JPanel {
    private int draggedIndex = -1;
    private int lineY = -1;
    private boolean showCheckboxes=false;

    public InteractiveComponentList() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        new DropTarget(this, DnDConstants.ACTION_MOVE, new PanelDragAndDropHandler(), true);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                dispatchEventToParent(e);
            }
        });
    }

    /**
     * Enable or disable the selection checkboxes.
     * @param showCheckboxes true to show checkboxes, false to hide them.
     */
    public void enableSelection(boolean showCheckboxes) {
    	this.showCheckboxes = showCheckboxes;
        for(int i=0;i<getComponentCount();++i) {
            showCheckboxForMiddle((InteractiveComponentListMiddle)getComponent(i));
        }
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if(!(comp instanceof InteractiveComponentListMiddle)) {
            comp = createInnerPanel(comp);
        }
        super.addImpl(comp, constraints,index);
        revalidate();
        repaint();
    }

    @Override
    public void remove(Component comp) {
        synchronized (getTreeLock()) {
            for(int i=0;i<getComponentCount();++i) {
                var p = (InteractiveComponentListMiddle)getComponent(i);
                if(p == comp || p.getInnerComponent() == comp) {
                    super.remove(i);
                    revalidate();
                    repaint();
                    return;
                }
            }
        }
    }

    private void dispatchEventToParent(MouseEvent e) {
        Container parent = getParent();
        if (parent != null) {
            Point parentPoint = SwingUtilities.convertPoint(this, e.getPoint(), parent);
            MouseEvent parentEvent = new MouseEvent(parent, e.getID(), e.getWhen(), e.getModifiersEx(), parentPoint.x, parentPoint.y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
            parent.dispatchEvent(parentEvent);
        }
    }

    private InteractiveComponentListMiddle createInnerPanel(Component component) {
        InteractiveComponentListMiddle panel = new InteractiveComponentListMiddle(component);
        panel.getHandle().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JComponent c = (JComponent) e.getSource();
                TransferHandler handler = ((JPanel) c.getParent()).getTransferHandler();
                if (handler != null) {
                    handler.exportAsDrag((JComponent) c.getParent(), e, TransferHandler.MOVE);
                    draggedIndex = getComponentZOrder(c.getParent());
                }
            }
        });

        showCheckboxForMiddle(panel);

        return panel;
    }

    private void showCheckboxForMiddle(InteractiveComponentListMiddle p) {
        p.getCheck().setVisible(showCheckboxes);
        if(!showCheckboxes) p.getCheck().setSelected(false);
    }

    public void updateLineIndicator(int mouseY) {
        lineY = mouseY;
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (lineY >= 0) {
            g.setColor(Color.BLUE);
            g.fillRect(0, lineY - 2, getWidth(), 4);
        }
    }

    public void moveDroppableHere(int lineY) {
        int dropIndex = getDropIndex(new Point(0, lineY));
        // Adjust dropIndex if necessary
        dropIndex = dropIndex > draggedIndex ? dropIndex - 1 : dropIndex;
        if (dropIndex != draggedIndex) {
            Component draggedComponent = getComponent(draggedIndex);
            remove(draggedComponent);
            add(draggedComponent, dropIndex);
            revalidate();
            repaint();
            draggedIndex = dropIndex; // Update draggedIndex

            fireMoveEvent(draggedIndex,dropIndex);
        }
    }

    public void addListener(ListDataListener listener) {
        listenerList.add(ListDataListener.class, listener);
    }

    public void removeListener(ListDataListener listener) {
        listenerList.remove(ListDataListener.class, listener);
    }

    /**
     * Fire a move event to all listeners.
     * @param draggedIndex the from index
     * @param dropIndex the to index
     */
    private void fireMoveEvent(int draggedIndex, int dropIndex) {
        ListDataListener[] listeners = listenerList.getListeners(ListDataListener.class);
        ListDataEvent e = null;
        for (ListDataListener listener : listeners) {
            if(e==null) {
                // lazy init - if no listeners we don't waste time allocating ram.
                e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, draggedIndex, dropIndex);
            }
            listener.contentsChanged(e);
        }
    }

    private class PanelDragAndDropHandler extends DropTargetAdapter {
        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            Point dropPoint = dtde.getLocation();
            updateLineIndicator(dropPoint.y);
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            moveDroppableHere(lineY);
            removeLineIndicator();
        }

        @Override
        public void dragExit(DropTargetEvent dte) {
            removeLineIndicator();
        }
    }

    /**
     * Get the index of the component that should be dropped at the given point.
     * @param dropPoint the point where the component should be dropped
     * @return the index of the component that should be dropped at the given point
     */
    public int getDropIndex(Point dropPoint) {
        for (int i = 0; i < getComponentCount(); i++) {
            Rectangle bounds = getComponent(i).getBounds();
            if (dropPoint.y < bounds.y + bounds.height / 2) {
                return i;
            }
        }
        return getComponentCount();
    }

    public void removeLineIndicator() {
        lineY = -1;
        repaint();
    }

    /**
     * Components are contained in {@link InteractiveComponentListMiddle}, which has a checkbox.  Find all components associated with
     * a selected checkbox.
     * @return all {@link Component}s in selected rows.
     */
    public List<Component> getSelectedComponents() {
        List<Component> selectedPanels = new ArrayList<>();
        for (Component c : getComponents()) {
            if (c instanceof InteractiveComponentListMiddle panel) {
                if (panel.getCheck().isSelected()) {
                    selectedPanels.add(panel.getInnerComponent());
                }
            }
        }
        return selectedPanels;
    }

    /**
     * Get the inner component of the {@link InteractiveComponentListMiddle} at the given index.
     * @param index the index
     * @return the inner component of the {@link InteractiveComponentListMiddle} at the given index.
     */
    public Component getInnerComponent(int index) {
        return ((InteractiveComponentListMiddle) getComponent(index)).getInnerComponent();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            InteractiveComponentList panel = new InteractiveComponentList();

            panel.add(new JButton("Item 1"));
            panel.add(new JButton("Item 2"));
            panel.add(new JButton("Item 3"));

            frame.add(new JScrollPane(panel));
            frame.setTitle("Drag-and-Drop Panels with Buttons");
            frame.setSize(200, 160);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
