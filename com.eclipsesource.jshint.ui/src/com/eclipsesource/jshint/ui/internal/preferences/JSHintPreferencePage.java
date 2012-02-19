/*******************************************************************************
 * Copyright (c) 2012 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package com.eclipsesource.jshint.ui.internal.preferences;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eclipsesource.jshint.ui.internal.Activator;
import com.eclipsesource.jshint.ui.internal.builder.BuilderUtil;
import com.eclipsesource.jshint.ui.internal.builder.JSHintBuilder;


public class JSHintPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private Button defaultLibButton;
  private Button customLibButton;
  private Text customLibPathText;
  private Button customLibPathButton;
  private boolean useCustomJSHint;
  private String customLibPath;

  public JSHintPreferencePage() {
    setPreferenceStore( Activator.getDefault().getPreferenceStore() );
    setDescription( "General settings for JSHint" );
  }

  public void init( IWorkbench workbench ) {
    loadPreferences();
  }

  @Override
  protected Control createContents( Composite parent ) {
    Composite composite = new Composite( parent, SWT.NONE );
    GridLayout mainLayout = createMainLayout();
    mainLayout.marginTop = 10;
    composite.setLayout( mainLayout );
    createCustomJSHintArea( composite );
    updateControls();
    return composite;
  }

  @Override
  public boolean performOk() {
    updateValuesFromControls();
    boolean dirty = savePreferences();
    if( dirty ) {
      try {
        triggerRebuild();
      } catch( CoreException exception ) {
        Activator.logError( "Failed to rebuild workspace", exception );
        return false;
      }
    }
    return true;
  }

  @Override
  protected void performDefaults() {
    IPreferenceStore store = getPreferenceStore();
    useCustomJSHint = store.getBoolean( PreferencesConstants.PREF_USE_CUSTOM_JSHINT );
    customLibPath = store.getString( PreferencesConstants.PREF_CUSTOM_JSHINT_PATH );
    updateControls();
    super.performDefaults();
  }

  private void createCustomJSHintArea( Composite parent ) {
    defaultLibButton = new Button( parent, SWT.RADIO );
    defaultLibButton.setText( "Use the &built-in JSHint library (version r05)" );
    defaultLibButton.setLayoutData( createFillData( 3 ) );
    customLibButton = new Button( parent, SWT.RADIO );
    customLibButton.setText( "Provide a &custom JSHint library file (JSLint is also supported)" );
    customLibButton.setLayoutData( createFillData( 3 ) );
    customLibButton.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        updateValuesFromControls();
        updateControls();
      }
    } );
    customLibPathText = new Text( parent, SWT.BORDER );
    GridData textData = createFillData( 2 );
    textData.horizontalIndent = 25;
    customLibPathText.setLayoutData( textData );
    customLibPathButton = new Button( parent, SWT.PUSH );
    customLibPathButton.setText( "Select" );
    customLibPathButton.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        selectFile();
      }
    } );
    Text customLibPathLabelText = new Text( parent, SWT.READ_ONLY | SWT.WRAP );
    customLibPathLabelText.setText( "This file is usually named 'jshint.js' or 'jslint.js'." );
    customLibPathLabelText.setBackground( parent.getBackground() );
    GridData labelTextData = createFillData( 2 );
    labelTextData.horizontalIndent = 25;
    customLibPathLabelText.setLayoutData( labelTextData );
  }

  private void selectFile() {
    FileDialog fileDialog = new FileDialog( getShell(), SWT.OPEN );
    fileDialog.setText( "Select JSHint library file" );
    File file = new File( customLibPath );
    fileDialog.setFileName( file.getName() );
    fileDialog.setFilterPath( file.getParent() );
    fileDialog.setFilterNames( new String[] { "JavaScript files" } );
    fileDialog.setFilterExtensions( new String[] { "*.js", "" } );
    String selectedPath = fileDialog.open();
    if( selectedPath != null ) {
      customLibPath = selectedPath;
      updateControls();
    }
  }

  private void updateValuesFromControls() {
    useCustomJSHint = customLibButton.getSelection();
    customLibPath = customLibPathText.getText();
  }

  private void updateControls() {
    defaultLibButton.setSelection( !useCustomJSHint );
    customLibButton.setSelection( useCustomJSHint );
    customLibPathText.setText( customLibPath );
    customLibPathText.setEnabled( useCustomJSHint );
    customLibPathButton.setEnabled( useCustomJSHint );
  }

  private void loadPreferences() {
    IPreferenceStore store = getPreferenceStore();
    useCustomJSHint = store.getBoolean( PreferencesConstants.PREF_USE_CUSTOM_JSHINT );
    customLibPath = store.getString( PreferencesConstants.PREF_CUSTOM_JSHINT_PATH );
  }

  private boolean savePreferences() {
    boolean dirty = false;
    IPreferenceStore store = getPreferenceStore();
    if( useCustomJSHint != store.getBoolean( PreferencesConstants.PREF_USE_CUSTOM_JSHINT ) ) {
      store.setValue( PreferencesConstants.PREF_USE_CUSTOM_JSHINT, useCustomJSHint );
      dirty = true;
    }
    if( !customLibPath.equals( store.getString( PreferencesConstants.PREF_CUSTOM_JSHINT_PATH ) ) ) {
      store.setValue( PreferencesConstants.PREF_CUSTOM_JSHINT_PATH, customLibPath );
      dirty = true;
    }
    return dirty;
  }

  private void triggerRebuild() throws CoreException {
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for( IProject project : projects ) {
      if( project.isAccessible() ) {
        BuilderUtil.triggerClean( project, JSHintBuilder.ID );
      }
    }
  }

  private static GridLayout createMainLayout() {
    GridLayout layout = new GridLayout( 3, false );
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    return layout;
  }

  private static GridData createFillData( int span ) {
    GridData data = new GridData( SWT.FILL, SWT.CENTER, true, false );
    data.horizontalSpan = span;
    return data;
  }

}
