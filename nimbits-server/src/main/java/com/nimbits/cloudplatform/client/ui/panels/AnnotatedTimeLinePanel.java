/*
 * Copyright (c) 2013 Nimbits Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.  See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.cloudplatform.client.ui.panels;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.dnd.DND;
import com.extjs.gxt.ui.client.dnd.DropTarget;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.TreeStoreModel;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.WindowMode;
import com.nimbits.cloudplatform.client.common.Utils;
import com.nimbits.cloudplatform.client.constants.Const;
import com.nimbits.cloudplatform.client.constants.Words;
import com.nimbits.cloudplatform.client.enums.EntityType;
import com.nimbits.cloudplatform.client.model.GxtModel;
import com.nimbits.cloudplatform.client.model.TreeModel;
import com.nimbits.cloudplatform.client.model.common.CommonIdentifier;
import com.nimbits.cloudplatform.client.model.common.impl.CommonFactory;
import com.nimbits.cloudplatform.client.model.entity.Entity;
import com.nimbits.cloudplatform.client.model.entity.EntityName;
import com.nimbits.cloudplatform.client.model.timespan.Timespan;
import com.nimbits.cloudplatform.client.model.timespan.TimespanModelFactory;
import com.nimbits.cloudplatform.client.model.timespan.TimespanServiceClientImpl;
import com.nimbits.cloudplatform.client.model.value.Value;
import com.nimbits.cloudplatform.client.service.value.ValueService;
import com.nimbits.cloudplatform.client.service.value.ValueServiceAsync;
import com.nimbits.cloudplatform.client.ui.helper.FeedbackHelper;
import com.nimbits.cloudplatform.client.ui.icons.Icons;

import java.util.*;
@SuppressWarnings("unchecked")
public class AnnotatedTimeLinePanel extends LayoutContainer {
    private static final int ENTER_KEY = 13;
    private final DateTimeFormat fmt = DateTimeFormat.getFormat(Const.FORMAT_DATE_TIME);

    private AnnotatedTimeLine line;
    private ContentPanel mainPanel;
    private DataTable dataTable = null;
    private final Map<EntityName, Entity> points;
    private final Map<EntityName, List<Value>> valueMap;
    private final TextField endDateSelector;
    private final TextField startDateSelector;
    private final List<DropListener> dropListeners;
    private final List<ChartRemovedListener> chartRemovedListeners;

    private Timespan timespan;
    private boolean headerVisible;
    private final String name;
    private boolean selected;
    private static final String DEFAULT_EMPTY_COL = "EMPTY";

    @Override
    protected void onResize(final int width, final int height) {
        super.onResize(width, height);
        refreshSize(width);
    }

    // ChartRemoved Click Handlers
    public interface ChartRemovedListener {
        void onChartRemovedClicked();
    }



    void notifyChartRemovedListener() {
        for (final ChartRemovedListener ChartRemovedClickedListener : chartRemovedListeners) {
            ChartRemovedClickedListener.onChartRemovedClicked();
        }
    }

    // Drop Click Handlers
    public interface DropListener {
        void onDrop();
    }

    void addDropListeners(final DropListener listener) {
        dropListeners.add(listener);
    }

    void notifyDropListener() {
        for (final DropListener DropClickedListener : dropListeners) {
            DropClickedListener.onDrop();
        }
    }


    public AnnotatedTimeLinePanel(final boolean showHeader, final String name) {
        this.headerVisible = showHeader;
        this.name = name;
        points = new HashMap<EntityName, Entity>(10);
        valueMap = new HashMap<EntityName, List<Value>>(100);
        endDateSelector = new TextField();
        startDateSelector = new TextField();
        chartRemovedListeners = new ArrayList<ChartRemovedListener>(1);
        dropListeners = new ArrayList<DropListener>(1);
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setBorders(selected);

    }
    //data

    private void addPointDataToTable(final TreeModel entity, final List<Value> values)  {

        removePointDataFromTable(CommonFactory.createName(DEFAULT_EMPTY_COL, EntityType.point));
        if (dataTable == null) {
            initTable();
        }
        final int r = dataTable.getNumberOfColumns();
        int currentRow = dataTable.getNumberOfRows();
        int PointColumn = dataTable.getNumberOfColumns();
        boolean found = false;
        for (int i = 0; i < r; i++) {
            final String s = dataTable.getColumnLabel(i);
            if (s.equals(entity.getName().getValue())) {
                PointColumn = i;
                found = true;
                break;
            }
        }


        if (!found) {
            dataTable.addColumn(ColumnType.NUMBER, entity.getName().getValue());
            dataTable.addColumn(ColumnType.STRING, "title" + r);
            dataTable.addColumn(ColumnType.STRING, "text" + r);
        }

        if (values != null) {
            if (valueMap.containsKey(entity.getName())) {
                List<Value> list =   valueMap.get(entity.getName());
                if (list == null) {
                    List<Value> valueArrayList = new ArrayList<Value>(values.size());
                    valueArrayList.addAll(values);
                    valueMap.remove(entity.getName());
                    valueMap.put(entity.getName(), valueArrayList);
                }
                else {
                    valueMap.get(entity.getName()).addAll(values);
                }
            }
            else {
                valueMap.put(entity.getName(), values);
            }
            for (final Value v : values) {

//                points.get(entity.getName()).getValues().add(v);

                dataTable.addRow();
                dataTable.setValue(currentRow, 0, v.getTimestamp());
                dataTable.setValue(currentRow, PointColumn, v.getDoubleValue());

                String note = v.getNote();
                String name = entity.getName().getValue();

                if (Utils.isEmptyString(note)) {
                    note = "";
                    name = "";
                }

                //note = null;
                dataTable.setValue(currentRow, PointColumn + 2, note);
                dataTable.setValue(currentRow, PointColumn + 1, name);

                currentRow++;
            }
        }
    }

    private void removePointDataFromTable(final CommonIdentifier pointName) {
        if (dataTable != null) {
            int r = dataTable.getNumberOfColumns();
            for (int i = 0; i < r; i++) {
                String s = dataTable.getColumnLabel(i);
                if (s.equals(pointName.getValue())) {
                    dataTable.removeColumns(i, i + 2);
                    break;
                }
            }
        }

    }

    //end data


    public void addValue(final TreeModel model, final Value value)  {
        if (points.isEmpty() || points.containsKey(model.getName()))  {
            if (timespan != null) {
                Date end = timespan.getEnd().getTime() > value.getTimestamp().getTime() ? value.getTimestamp() : timespan.getEnd();
                Date start = timespan.getStart().getTime() < value.getTimestamp().getTime() ? value.getTimestamp() : timespan.getStart();
                if (value.getTimestamp().getTime() < start.getTime()) {
                    start = value.getTimestamp();
                }
                if (value.getTimestamp().getTime() > end.getTime()) {
                    end = value.getTimestamp();
                }
                this.timespan = TimespanModelFactory.createTimespan(start, end);

            } else {
                this.timespan =TimespanModelFactory.createTimespan(value.getTimestamp(), new Date());


            }
            startDateSelector.setValue(fmt.format(this.timespan.getStart()));
            endDateSelector.setValue(fmt.format(this.timespan.getEnd()));
            addPointDataToTable(model, Arrays.asList(value));

            drawChart();
        }
    }

    private void drawChart() {
        layout();
        line.draw(dataTable, createOptions());

    }

    @Override
    protected void onRender(final Element parent, final int index) {
        super.onRender(parent, index);



        setLayout(new FillLayout());
        mainPanel = new ContentPanel();
        mainPanel.setBodyBorder(false);
        mainPanel.setHeaderVisible(headerVisible);

        mainPanel.setFrame(false);
        mainPanel.setTopComponent(toolbar());
        mainPanel.setHeight("100%");

        if (headerVisible) {
            mainPanel.setHeading(name);


        }
        setDropTarget(mainPanel);
        add(mainPanel);
        initChart();
        layout(true);
    }


    private void initChart() {
        Runnable onLoadCallback = new Runnable() {
            @Override
            public void run() {
                if (line != null) {
                    mainPanel.remove(line);
                    line = null;
                }
                initTable();
                line = new AnnotatedTimeLine(dataTable, createOptions(), "100%", "100%");
                mainPanel.removeAll();
                mainPanel.add(line);
                addEmptyDataToTable();

                layout();
                if (points != null && !points.isEmpty()) {

                    timespan = TimespanServiceClientImpl.createTimespan(startDateSelector.getValue().toString(), endDateSelector.getValue().toString());

                    if (line != null && timespan != null) {

                        initTable();

                        for (Entity entity : points.values()) {
                            addEntityModel(new GxtModel(entity));
                        }
                    }

                }
            }
        };

        VisualizationUtils.loadVisualizationApi(onLoadCallback,
                AnnotatedTimeLine.PACKAGE);
    }

    private void initTable() {
        dataTable = DataTable.create();
        dataTable.addColumn(ColumnType.DATETIME, Words.WORD_DATE);
    }

    private void addEmptyDataToTable() {
        dataTable.addColumn(ColumnType.NUMBER, DEFAULT_EMPTY_COL);
        dataTable.addColumn(ColumnType.STRING, "title0");
        dataTable.addColumn(ColumnType.STRING, "text0");
    }

    public void refreshSize(int width) {
        if (width > 0 && line != null) {
            Runnable onLoadCallback = new Runnable() {
                @Override
                public void run() {
                    mainPanel.remove(line);
                    line = new AnnotatedTimeLine(dataTable, createOptions(), "100%", "100%");
                    mainPanel.add(line);
                    doLayout();
                }
            };

            VisualizationUtils.loadVisualizationApi(onLoadCallback,
                    AnnotatedTimeLine.PACKAGE);
        }
    }


    public void addEntityModel(TreeModel model) {
        //  Entity entity = model.getBaseEntity();
        if (!points.containsKey(model.getName()) && points.size() < 10) {
            if (model.getEntityType().equals(EntityType.point)) {
                points.put(model.getName(), model.getBaseEntity());
                addPointToChart(model);
            }
        }
        for (ModelData child : model.getChildren()) {
            addEntityModel((TreeModel) child);
        }
    }

    private void addPointToChart(final TreeModel model) {

        if (timespan == null) {
            loadValuesThatExist(model);
        } else {
            loadMemCache(model);
            final int start = 0;
            final int end = 1000;
            loadDataSegment(model, start, end);
        }
    }

    private void loadValuesThatExist(final TreeModel model) {
        final ValueServiceAsync dataService = GWT.create(ValueService.class);

        dataService.getTopDataSeriesRpc(model.getBaseEntity(), 100, new Date(), new TopSeriesListAsyncCallback(model));
    }

    public void setTimespan(Timespan ts) {
        this.timespan = ts;
        this.startDateSelector.setValue(fmt.format(ts.getStart()));
        this.endDateSelector.setValue(fmt.format(ts.getEnd()));
    }

    private void loadDataSegment(final TreeModel p, final int start, final int end) {
        final ValueServiceAsync dataService = GWT.create(ValueService.class);
        final MessageBox box = MessageBox.wait("Progress",
                "Loading " + p.getName().getValue() + " archived values " + start + " to " + end, "Loading...");
        box.show();
        //   Timespan timespan = new TimespanModel(startDate, endDate);
        dataService.getPieceOfDataSegmentRpc(p.getBaseEntity(), timespan, start, end, new GetSegmentAsyncCallback(box, p, end));
    }

    private void loadMemCache(final TreeModel model) {
        final ValueServiceAsync dataService = GWT.create(ValueService.class);
        final MessageBox box = MessageBox.wait("Progress",
                "Loading Buffered Data", "Loading...");
        box.show();

        dataService.getCacheRpc(model.getBaseEntity(), new GetMemCacheListAsyncCallback(box, model));


    }

    private void setDropTarget(final Component container) {
        //    DropTarget target = new DropTarget(container) {
        new LineDropTarget(container);
    }

    private void handleDrop(final TreeModel p) {
        if (p.getEntityType().equals(EntityType.point)) {
            addEntityModel(p);
        }
        for (final ModelData x : p.getChildren()) {
            handleDrop((TreeModel)x);
        }
        notifyDropListener();
    }

    private static Options createOptions() {
        final Options options = Options.create();
        options.setDisplayAnnotations(true);
        options.setWindowMode(WindowMode.OPAQUE);
        //options.
        //TODO options.setDisplayRangeSelector(true);

        //options.setDisplayAnnotationsFilter(arg0)
        return options;
    }

    public void removePoint(final Entity entity)  {
        removePointDataFromTable(entity.getName());
        if (points.containsKey(entity.getName())) {
            points.remove(entity.getName());
        }
        if (points.isEmpty()) {
            try {
                removePointDataFromTable(CommonFactory.createName(DEFAULT_EMPTY_COL, EntityType.point));
                addEmptyDataToTable();
            } catch (Exception e) {
                FeedbackHelper.showError(e);
            }

        }

        drawChart();
    }


    private ToolBar toolbar() {
        final ToolBar toolBar = new ToolBar();
        final Button resetChartButton = resetChartButton();
        final Button refresh = refreshButton();


        final Button startDateMenu = new Button();
        startDateMenu.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.calendar()));
        initStartDateSelector();
        initEndDateSelector();

        toolBar.add(startDateSelector);

        toolBar.add(endDateSelector);

        toolBar.add(refresh);

        toolBar.add(new SeparatorToolItem());

        final NumberField min = new NumberField();
        final NumberField max = new NumberField();
        Label minY = new Label("MinY:");
        Label maxY = new Label("MaxY:");
        min.setWidth(30);
        max.setWidth(30);

        min.setValue(0);
        max.setValue(100);

        toolBar.add(minY);
        toolBar.add(min);
        toolBar.add(maxY);
        toolBar.add(max);


        Button refreshRange = refreshRangeButton(min, max);
        toolBar.add(refreshRange);
        toolBar.add(new SeparatorToolItem());
        toolBar.add(resetChartButton);


        return toolBar;
    }

    private Button resetChartButton() {
        Button button = new Button();
        button.setToolTip("Reset Chart");
        button.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.delete()));
        button.addListener(Events.OnClick, new Listener<BaseEvent>() {
            @Override
            public void handleEvent(BaseEvent be) {
                for (Entity entity : points.values()) {
                    try {
                        removePoint(entity);
                    } catch (Exception e) {
                        FeedbackHelper.showError(e);
                    }
                }

            }
        });
        return button;
    }

    private Button refreshRangeButton(final NumberField min, final NumberField max) {
        Button refreshRange = new Button();
        refreshRange.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.refresh2()));
        refreshRange.addListener(Events.OnClick, new Listener<BaseEvent>() {
            @Override
            public void handleEvent(BaseEvent be) {
                Options options = Options.create();
                options.setDisplayAnnotations(true);
                options.setWindowMode(WindowMode.OPAQUE);
                // options.setAllowRedraw(true);
                // options.setDisplayRangeSelector(true);

                int mn = min.getValue().intValue();
                int mx = max.getValue().intValue();

                // options.set
                options.setMin(mn);


                line.draw(dataTable, options);
            }
        });
        return refreshRange;
    }

    private void initEndDateSelector() {
        if (timespan != null) {
            endDateSelector.setValue(fmt.format(timespan.getEnd()));
        }
        endDateSelector.setSelectOnFocus(false);
        endDateSelector.setToolTip("End Date");
        endDateSelector.addListener(Events.KeyPress, new Listener<FieldEvent>() {
            @Override
            public void handleEvent(FieldEvent be) {
                if (be.getKeyCode() == ENTER_KEY) {

                    initChart();

                }
            }
        });
    }

    private void initStartDateSelector() {
        startDateSelector.setSelectOnFocus(false);
        if (timespan != null) {
            startDateSelector.setValue(fmt.format(timespan.getStart()));
        }
        startDateSelector.setToolTip("Start Date");

        startDateSelector.addListener(Events.KeyPress, new Listener<FieldEvent>() {
            @Override
            public void handleEvent(FieldEvent be) {
                if (be.getKeyCode() == ENTER_KEY) {

                    initChart();

                }
            }
        });
    }

    private Button refreshButton() {
        final Button refresh = new Button();
        refresh.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.refresh2()));
        refresh.addListener(Events.OnClick, new Listener<BaseEvent>() {
            @Override
            public void handleEvent(final BaseEvent be) {

                initChart();

            }
        });
        return refresh;
    }



    private class TopSeriesListAsyncCallback implements AsyncCallback<List<Value>> {
        private final TreeModel model;

        private TopSeriesListAsyncCallback(TreeModel model) {
            this.model = model;
        }

        @Override
        public void onFailure(Throwable caught) {
            GWT.log(caught.getMessage());
        }

        @Override
        public void onSuccess(final List<Value> result) {

            if (!result.isEmpty()) {
                Value oldest = result.get(result.size() - 1);


                Value newest = result.get(0);


                timespan = TimespanModelFactory.createTimespan(oldest.getTimestamp(), newest.getTimestamp());
                setTimespan(timespan);
            }


            try {
                addPointDataToTable(model, result);
            } catch (Exception e) {
                FeedbackHelper.showError(e);
            }

            drawChart();


            // box.close();
        }
    }

    private class GetSegmentAsyncCallback implements AsyncCallback<List<Value>> {
        private final MessageBox box;
        private final TreeModel p;
        private final int end;

        private GetSegmentAsyncCallback(MessageBox box, TreeModel p, int end) {
            this.box = box;
            this.p = p;
            this.end = end;
        }

        @Override
        public void onFailure(final Throwable caught) {
            box.close();
            FeedbackHelper.showError(caught);
        }

        @Override
        public void onSuccess(final List<Value> result) {
            try {
                addPointDataToTable(p, result);
            } catch (Exception e) {
                FeedbackHelper.showError(e);
            }
            if (!result.isEmpty()) {
                if (end < 10000) {
                    loadDataSegment(p, end + 1, end + 1000);
                }
                else {
                    drawChart();
                }
            } else {
                drawChart();
            }

            box.close();
        }
    }

    private class GetMemCacheListAsyncCallback implements AsyncCallback<List<Value>> {
        private final MessageBox box;
        private final TreeModel model;

        private GetMemCacheListAsyncCallback(MessageBox box, TreeModel model) {
            this.box = box;
            this.model = model;
        }

        @Override
        public void onFailure(final Throwable caught) {
            FeedbackHelper.showError(caught);
            box.close();
        }

        @Override
        public void onSuccess(final List<Value> result) {
            try {
                addPointDataToTable(model, result);
            } catch (Exception e) {
                box.close();
                FeedbackHelper.showError(e);
            }

            box.close();
        }
    }

    private class LineDropTarget extends DropTarget {
        private LineDropTarget(Component container) {
            super(container);
        }

        @Override
        protected void onDragDrop(final DNDEvent event) {
            super.onDragDrop(event);
            super.setOperation(DND.Operation.COPY);
            event.setOperation(DND.Operation.COPY);
            List<TreeStoreModel> t = event.getData();
            for (final TreeStoreModel a : t) {
                TreeModel p = (TreeModel) a.getModel();
                handleDrop(p);
            }
        }
    }


}
