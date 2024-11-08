package de.nmichael.efa.gui;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Special dialog to add additional items while being in process of reserving items (boat or other items).
 */
public class ReserveAdditionalsDialog extends BaseDialog{
//Done: Rückgabe der selektierten Boote an die Reservierungslogik
//Todo: Sortierung der Listen
//Done: Bereits reservierte Boote werden nicht zur Auswahl angezeigt
//Done: Boot von welchem die ursprüngliche Reservierung ausgeht, sollte möglichst in rechter Liste sein - darf zumindest aber nicht in linker Liste auftauchen
//Done: Internationalisierung
//Todo: Feature Toggle um zwischen alter und neuer Logik umzuschalten
//Done: für normale Nutzer soll die Gruppe der eigentlichen Reservierung vorselektiert und der Wechsel der GroupDropDown nicht möglich sein
//Done: Initial die Gruppe der Hauptselektion auswählen
//Todo: Tests

    public static final String BOAT_SELECT_BTN = "boat_select_btn";
    public static final String BOAT_ALL_SELECT_BTN = "boat_select_all_btn";
    public static final String BOAT_REMOVE_BTN = "boat_remove_btn";
    private final Hashtable<String, TableItem[]> items;
    private final boolean isAdminMode;

    private String KEYACTION_ENTER;

    private String[] boatSeatsValuesArray = EfaTypes
            .makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES); //Bootskategorien/-gruppen : keys (1,2,3,...)
    private String[] boatSeatsDisplayArray = EfaTypes
            .makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY);//Bootskategorien/-gruppen : Werte (Canadier, SUPs,..)

    private ItemTypeStringList kategorienDropDown;

    private ItemTypeLabel label_caution;

    private ItemTypeList<IItemType> itemsOfGroupList;

    private ItemTypeList<IItemType> selectedItemsList;

    private ItemTypeButton selectButton;

    private ItemTypeButton selectAllButton;

    private ItemTypeButton removeButton;

    private ItemTypeStringList groupDropDown;

    private HashMap<String, FilteringModel> groupsData;

    private BoatReservationRecord originalReservation;

    /**
     * The sole constructor
     * @param parent
     * @param title
     * @param originalReservation
     * @param items
     * @param isAdminMode
     */
    public ReserveAdditionalsDialog(Window parent, String title, BoatReservationRecord originalReservation, Hashtable<String, TableItem[]> items, boolean isAdminMode) {
        // 2024-02-25 abf leider kein "abbrechen"-Button möglich. "nein Danke, mir reicht das eine Boot" --> cancel.
        super(parent, title, International.getStringWithMnemonic("OK")
                + ", keine " + ("Boote dazubuchen")); // ausgewählte
        this.items = items;
        this.originalReservation = originalReservation;
        this.isAdminMode = isAdminMode;
    }

    @Override
    public void keyAction(ActionEvent evt) {

    }

    @Override
    protected void iniDialog() throws Exception {
       KEYACTION_ENTER = addKeyAction("ENTER");

       mainPanel.setLayout(new GridBagLayout());

       groupsData = new HashMap<>();

       label_caution = new ItemTypeLabel("L0", IItemType.TYPE_INTERNAL, "",
               International.getString("de.nmichael.efa.gui.ReserveAdditionalsDialog.questionMoreReservations"));
       label_caution.displayOnGui(this, mainPanel, 0,0);

       String typeSeatsOfOriginalReservation = originalReservation.getBoat().getTypeSeats(0);

       groupDropDown = new ItemTypeStringList("Groups",typeSeatsOfOriginalReservation,  boatSeatsValuesArray, boatSeatsDisplayArray,0,"",null);
       groupDropDown.setFieldSize(300, 30);
       groupDropDown.displayOnGui(this, mainPanel, 1, 0);
       if (Daten.efaConfig.getValueEfaDirekt_allowAdvancedReserveBoatCategories()) {
           groupDropDown.setEnabled(true);
       } else {
           groupDropDown.setEnabled(isAdminMode);
       }

       //linke Liste: Zeigt Boote/Items der per dropDown ausgewählten Gruppe
       itemsOfGroupList = new ItemTypeList("Stuff",IItemType.TYPE_PUBLIC, "", International.getString("de.nmichael.efa.gui.ReserveAdditionalsDialog.availabeItems"));
       itemsOfGroupList.setPadding(0, 10, 0, 10);
       itemsOfGroupList.setFieldSize(300, 200);
       itemsOfGroupList.displayOnGui(this, mainPanel, 0, 1);

       //rechte Liste: zeigt die Boote/Items, welche zur Reservierung ausgewählt wurden.
       selectedItemsList = new ItemTypeList("SelectedStuff",IItemType.TYPE_PUBLIC, "", International.getString("de.nmichael.efa.gui.ReserveAdditionalsDialog.preview"));
       selectedItemsList.setPadding(0, 0, 0, 10);
       selectedItemsList.setFieldSize(300, 200);
       selectedItemsList.displayOnGui(this, mainPanel, 1, 1);

       selectButton = new ItemTypeButton(BOAT_SELECT_BTN,0, "",International.getString("de.nmichael.efa.gui.ReserveAdditionalsDialog.selectItem"));
       //selectButton.setFieldSize(20, 30);
       selectButton.setPadding(0, 10, 0, 0);
       selectButton.displayOnGui(this, mainPanel, 0, 2);

       selectAllButton = new ItemTypeButton(BOAT_ALL_SELECT_BTN,0, "",International.getString("de.nmichael.efa.gui.ReserveAdditionalsDialog.selectAllItems"));
       //selectAllButton.setFieldSize(20, 30);
       selectAllButton.setPadding(0, 10, 5, 0);
       selectAllButton.displayOnGui(this, mainPanel,0,3);
       selectAllButton.setEnabled(isAdminMode);

       removeButton = new ItemTypeButton(BOAT_REMOVE_BTN,0, "",International.getString("de.nmichael.efa.gui.ReserveAdditionalsDialog.removeItems"));
       //removeButton.setFieldSize(20, 30);
       removeButton.setPadding(0, 0, 0, 0);
       removeButton.displayOnGui(this, mainPanel,1,2);

        if (closeButton != null) {
            closeButton.setIcon(getIcon("button_accept.png"));
        }

        selectButton.registerItemListener(new AddRemoveListener(itemsOfGroupList, selectedItemsList) {
            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {

                List<IItemType> result = new ArrayList<>();
                if (itemType.getName().equals(BOAT_SELECT_BTN) && event instanceof ActionEvent) {
                    IItemType selectedItem = itemsOfGroupList.getSelectedValue();
                        result.add(selectedItem);
                }
                return result; //Liste mit selektierten Items
            }
        });

        selectAllButton.registerItemListener(new AddRemoveListener(itemsOfGroupList, selectedItemsList) {

            List<IItemType> result = new ArrayList<>();

            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {
                if (itemType.getName().equals(BOAT_ALL_SELECT_BTN) && event instanceof ActionEvent) {
                    for (int i = 0; i <= itemsOfGroupList.size() -1; i++) {
                        Object item = itemsOfGroupList.getItemObject(i);
                        if (item instanceof IItemType) {
                         result.add((ItemType) item);
                        }
                    }
                }
                return result; //Liste mit allen Items der Gruppe
            }
        });

        removeButton.registerItemListener(new AddRemoveListener(selectedItemsList, itemsOfGroupList) {
            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {
                //TODO: Zurücklegen soll nur möglich sein, wenn auch die entsprechende Kategorie aktiviert ist
                List<IItemType> result = new ArrayList<>();
                if (itemType.getName().equals(BOAT_REMOVE_BTN) && event instanceof ActionEvent) {
                    IItemType selectedItem = selectedItemsList.getSelectedValue();
                    result.add(selectedItem);
                }
                return result;
            }
        });

        //wählt Items mittels Doppelklick statt mit selectButton
        itemsOfGroupList.registerItemListener(new AddRemoveListener(itemsOfGroupList, selectedItemsList) {
            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {

                List<IItemType> result = new ArrayList<>();
                if(event instanceof ActionEvent )
                {
                    String actionCommand = ((ActionEvent) event).getActionCommand();
                    if (actionCommand.equals(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                        IItemType selectedItem = itemsOfGroupList.getSelectedValue();
                        result.add(selectedItem);
                    }
                }
                return result;
            }
        });

        //löscht Items mittels Doppelklick statt mit selectButton
        selectedItemsList.registerItemListener(new AddRemoveListener(selectedItemsList, itemsOfGroupList) {
            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {
                List<IItemType> result = new ArrayList<>();
                if (event instanceof ActionEvent ) {
                    String actionCommand = ((ActionEvent) event).getActionCommand();
                    if (actionCommand.equals(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                        IItemType selectedItem = selectedItemsList.getSelectedValue();
                        result.add(selectedItem);
                    }
                }
                return result;
            }
        });

        IItemListener groupDropDownListener = new IItemListener() {

            @Override
            /**
             * Listener um in Liste boatList/ die Boote der Kategorie zu zeigen, welche in der Dropdownleiste gewählt wurde
             */
            public void itemListenerAction(IItemType itemType, AWTEvent event) {
                if (event instanceof ItemEvent
                        && event.getID() == ItemEvent.ITEM_STATE_CHANGED && ((ItemEvent) event).getStateChange() == ItemEvent.SELECTED) {
                    Object selectedItem = ((ItemEvent) event).getItem();
                    if (selectedItem instanceof ItemTypeStringList.ItemLabelValue) {
                        String typeSeats = ((ItemTypeStringList.ItemLabelValue) selectedItem).getValue();
                        updateSelectableItemList(typeSeats);
                    }
                }
            }
        };

        groupDropDown.registerItemListener(groupDropDownListener);
        updateSelectableItemList(typeSeatsOfOriginalReservation);

    }

    /**
     * Fills and updates the list of selectable items by typeSeats
     *
     * @param typeSeats
     */
    private void updateSelectableItemList(String typeSeats){
        new Thread(new Runnable() {
            public void run() {
                FilteringModel currentModel = null;
                try {
                    currentModel = ReserveAdditionalsDialog.this.groupsData.get(typeSeats);
                    //Aufrufe an die Datenbank reduzieren

                    if (currentModel == null) {
                        ArrayList<IItemType> itemsByTypeSeats = ReserveAdditionalsDialog.this.createListOfItemsByTypeSeats(typeSeats);
                        currentModel = new FilteringModel(itemsByTypeSeats);
                        ReserveAdditionalsDialog.this.groupsData.put(typeSeats, currentModel);
                    }

                } catch (EfaException e) {
                    throw new RuntimeException(e);
                }

                FilteringModel finalCurrentModel = currentModel;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        //Nur mit gefilterten Items befüllen, damit nicht neu bestehendes Material/Boote ausgeliehen werden können
                        itemsOfGroupList.removeAllItems();
                        List<IItemType> selectedItems = selectedItemsList.getItemObjects();
                        List<IItemType> filteredList = finalCurrentModel.filter(selectedItems);
                        for (IItemType boat :filteredList) {
                            itemsOfGroupList.addItem(boat.getName(), boat, true, '\0');
                        }
                        itemsOfGroupList.sortAlphabetically();
                        itemsOfGroupList.requestFocus();
                    }
                });
            }
        }).start();
    }

    //TODO: schön machen...

    /**
     * Retrieves items by given typeSeats identifier
     * @param typeSeats
     * @return
     * @throws EfaException
     */
    private ArrayList<IItemType> createListOfItemsByTypeSeats(String typeSeats)
            throws  EfaException{
        IDataAccess allBoats = Daten.project.getBoats(false).data();
        ArrayList<IItemType> liste = new ArrayList<>();
        long now = System.currentTimeMillis();

        BoatReservations reservations = Daten.project.getBoatReservations(false); //bisherige vorherige Reservierungen

        List<String> keys = Collections.list(items.keys());

        for (DataKey<UUID, Long, String> dataKey : allBoats.getAllKeys()) {
            BoatRecord boatRecord = (BoatRecord) allBoats.get(dataKey);

            if(!originalReservation.getBoat().getId().equals(boatRecord.getId()) ) {
                //zuvor gewähltes Boot soll nicht in boatList (weitere Boote reservieren) auftauchen

                if (!boatRecord.isValidAt(now)) {
                    // Boot nicht mehr gültig, abgelaufen
                    continue;
                }

                if (!typeSeats.equals(boatRecord.getTypeSeats(0))) {
                    // aber nicht fremde Bootstypen - hier als Sitzplätze
                    // für andere Bootstypen, bitte neue Reservierung dort aufmachen.
                    continue;
                }

                try{
                    BoatReservationRecord testReservationsRecord = reservations
                            .createBoatReservationsRecordFromClone(boatRecord.getId(), originalReservation);

                    reservations.preModifyRecordCallback(testReservationsRecord, true, false, false);
                    String prefixError = "";
                    if (!typeSeats.equals(boatRecord.getTypeSeats(0))) {
                        prefixError = typeSeats + "=" + boatRecord.getTypeSeats(0) + ": ";
                        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, prefixError + "beim Übertragen auf weitere Gruppen");
                    }
                    ItemTypeBoolean item = new ItemTypeBoolean(prefixError + boatRecord.getName(), false,
                            IItemType.TYPE_INTERNAL, "", boatRecord.getQualifiedName());
                    item.setDataKey(boatRecord.getKey());
                    liste.add(item);
                }catch (EfaModifyException exception){
                    //Intentionally left blank
                }
            }
        }
        return liste;
    }

    public static List<IItemType> showInputDialog(Window parent, BoatReservationRecord originalReservation, Hashtable<String, TableItem[]> items, boolean adminMode) {
        ReserveAdditionalsDialog dlg = new ReserveAdditionalsDialog(parent, "Übertragen auf weitere Gruppen", originalReservation, items, adminMode);
        dlg.showDialog();
        return dlg.selectedItemsList.getItemObjects();
    }

    /**
     * Abstract listener to add or remove items from {@link ItemTypeList}s
     */
    private abstract class AddRemoveListener implements IItemListener{

        private ItemTypeList source;
        private ItemTypeList target;

        public AddRemoveListener(ItemTypeList source, ItemTypeList target) {
            this.source = source;
            this.target = target;
        }
        //Verschiebt Items von einer Liste zur anderen Liste
        @Override
        public void itemListenerAction(IItemType itemType, AWTEvent event) {
            List<IItemType> selectedItems = getSelectedItems(itemType, event);
            for (IItemType selectedItem : selectedItems){
                if (target != null){
                    String selectedCategory = groupDropDown.getValue();
                    //Nur hinzufügen wenn auch die korrekte Kategorie aktuell ausgewählt wurde
                    FilteringModel currentModel = groupsData.get(selectedCategory);
                    if (selectedItem instanceof ItemTypeBoolean){
                        ((ItemTypeBoolean) selectedItem).setValue(true);
                    }
                    if (currentModel != null & currentModel.filter(null).contains(selectedItem)){
                        target.addItem(selectedItem.getName(), selectedItem, true,'\0' );
                    }
                }
                if (source != null & selectedItems.size() == 1){
                    source.removeItem(source.getSelectedIndex());
                } else {
                    source.removeAllItems();
                }
            }
            itemsOfGroupList.sortAlphabetically();  //werden die Elemente entfernt, sollen sie geordnet angezeigt werden
            updateOkSaveButtonText(itemType.getName()); // buttonPressed
            if (target == null){
                target.requestFocus();
            }
            if (source == null){
                source.requestFocus();
            }
            selectedItems.clear();
        }

        private void updateOkSaveButtonText(String areaClicked) {
            String anzahlZusaetzlich = "0";
            if (areaClicked.equals(BOAT_REMOVE_BTN)
                    || areaClicked.equals("SelectedStuff")) {
                if (source != null) {
                    anzahlZusaetzlich = "" + source.getItemObjects().size();
                }
            } else {
                if (target != null) {
                    anzahlZusaetzlich = "" + target.getItemObjects().size();
                }
            }
            if (anzahlZusaetzlich.endsWith("0")) {
                anzahlZusaetzlich = "keine";
            }
            _closeButtonText = International.getStringWithMnemonic("Speichern")
                    + " und dabei " + anzahlZusaetzlich + " "
                    + International.getString("Boote dazubuchen"); // ausgewählte
            closeButton.setText(_closeButtonText);
        }

        protected abstract List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event);
    }

    /**
     * Data model for group specific items. Items may be filtered.
     */
    private static class FilteringModel {
        List<IItemType> originList;
        List<IItemType> filteredList;

        public FilteringModel() {
            this(new ArrayList<>());
        }

        public FilteringModel(ArrayList<IItemType> originList){
            this.originList = originList;
            filteredList = new ArrayList<>();
        }

        public void addElement(IItemType element) {
            originList.add(element);
        }

        public List<IItemType> filter(Collection<IItemType> itemTypes) {
            if (itemTypes == null){
                return originList;
            }
            return originList.stream().filter(item -> !itemTypes.contains(item)).collect(Collectors.toList());
        }
    }

}
