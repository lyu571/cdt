/*
 *(c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 * 
 */
package org.eclipse.cdt.debug.internal.ui.preferences;

import java.text.MessageFormat;

import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.ICDebugConstants;
import org.eclipse.cdt.debug.core.cdi.ICDIFormat;
import org.eclipse.cdt.debug.internal.ui.ICDebugHelpContextIds;
import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.cdt.debug.ui.ICDebugUIConstants;
import org.eclipse.cdt.utils.ui.controls.ControlFactory;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * 
 * Preference page for debug preferences that apply specifically to
 * C/C++ Debugging.
 * 
 * @since Oct 3, 2002
 */
public class CDebugPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
	// View setting widgets
	private Button fPathsButton;
	private Button fRefreshRegistersButton;
	private Button fRefreshSolibsButton;
	private Combo fVariableFormatCombo;
	private Combo fExpressionFormatCombo;
	private Combo fRegisterFormatCombo;
	// Disassembly setting widgets
	private Button fAutoDisassemblyButton;
	
	// Maximum number of disassembly instructions to display
	private IntegerFieldEditor fMaxNumberOfInstructionsText;

	private static final int NUMBER_OF_DIGITS = 3;

	// Format constants
	private static int[] fFormatIds = new int[]{ ICDIFormat.NATURAL, ICDIFormat.HEXADECIMAL, ICDIFormat.DECIMAL };
	private static String[] fFormatLabels = new String[] { CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.Natural"), CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.Hexadecimal"), CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.Decimal") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private PropertyChangeListener fPropertyChangeListener;

	protected class PropertyChangeListener implements IPropertyChangeListener
	{
		private boolean fHasStateChanged = false;

		public void propertyChange( PropertyChangeEvent event )
		{
			if ( event.getProperty().equals( ICDebugPreferenceConstants.PREF_SHOW_HEX_VALUES ) )
			{
				fHasStateChanged = true;
			}
			else if ( event.getProperty().equals( ICDebugPreferenceConstants.PREF_SHOW_CHAR_VALUES ) )
			{
				fHasStateChanged = true;
			}
		}

		protected boolean hasStateChanged()
		{
			return fHasStateChanged;
		}
	}

	/**
	 * Constructor for CDebugPreferencePage.
	 */
	public CDebugPreferencePage()
	{
		super();
		setPreferenceStore( CDebugUIPlugin.getDefault().getPreferenceStore() );
		getPreferenceStore().addPropertyChangeListener( getPropertyChangeListener() );
		setDescription( CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.Preference_page_description") ); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents( Composite parent )
	{
		WorkbenchHelp.setHelp( getControl(), ICDebugHelpContextIds.C_DEBUG_PREFERENCE_PAGE );

		//The main composite
		Composite composite = new Composite( parent, SWT.NULL );
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout( layout );
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData( data );

		createSpacer( composite, 1 );
		createViewSettingPreferences( composite );
		createSpacer( composite, 1 );
		createDisassemblySettingPreferences( composite );
		setValues();

		return composite;
	}

	/**
	 * Creates composite group and sets the default layout data.
	 *
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @param labelText  the text label of the new composite
	 * @return the newly-created composite
	 */
	private Composite createGroupComposite( Composite parent, int numColumns, String labelText )
	{
		return ControlFactory.createGroup( parent, labelText, numColumns );
	}
		
	/**
	 * Set the values of the component widgets based on the
	 * values in the preference store
	 */
	private void setValues()
	{
		IPreferenceStore store = getPreferenceStore();
		fPathsButton.setSelection( store.getBoolean( ICDebugPreferenceConstants.PREF_SHOW_FULL_PATHS ) );
		fRefreshRegistersButton.setSelection( CDebugCorePlugin.getDefault().getPluginPreferences().getBoolean( ICDebugConstants.PREF_REGISTERS_AUTO_REFRESH ) );
		fRefreshSolibsButton.setSelection( CDebugCorePlugin.getDefault().getPluginPreferences().getBoolean( ICDebugConstants.PREF_SHARED_LIBRARIES_AUTO_REFRESH ) );
		fAutoDisassemblyButton.setSelection( CDebugCorePlugin.getDefault().getPluginPreferences().getBoolean( ICDebugConstants.PREF_AUTO_DISASSEMBLY ) );
		getMaxNumberOfInstructionsText().setStringValue( new Integer( CDebugCorePlugin.getDefault().getPluginPreferences().getInt( ICDebugConstants.PREF_MAX_NUMBER_OF_INSTRUCTIONS ) ).toString() );
		fVariableFormatCombo.select( getFormatIndex( CDebugCorePlugin.getDefault().getPluginPreferences().getInt( ICDebugConstants.PREF_DEFAULT_VARIABLE_FORMAT ) ) );
		fExpressionFormatCombo.select( getFormatIndex( CDebugCorePlugin.getDefault().getPluginPreferences().getInt( ICDebugConstants.PREF_DEFAULT_EXPRESSION_FORMAT ) ) );
		fRegisterFormatCombo.select( getFormatIndex( CDebugCorePlugin.getDefault().getPluginPreferences().getInt( ICDebugConstants.PREF_DEFAULT_REGISTER_FORMAT ) ) );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init( IWorkbench workbench )
	{
	}

	protected PropertyChangeListener getPropertyChangeListener()
	{
		if ( fPropertyChangeListener == null )
		{
			fPropertyChangeListener = new PropertyChangeListener();
		}
		return fPropertyChangeListener;
	}

	/**
	 * Set the default preferences for this page.
	 */
	public static void initDefaults(IPreferenceStore store)
	{
		store.setDefault( ICDebugPreferenceConstants.PREF_SHOW_HEX_VALUES, false );
		store.setDefault( ICDebugPreferenceConstants.PREF_SHOW_FULL_PATHS, true );
		CDebugCorePlugin.getDefault().getPluginPreferences().setDefault( ICDebugConstants.PREF_AUTO_DISASSEMBLY, false );
		CDebugCorePlugin.getDefault().getPluginPreferences().setDefault( ICDebugConstants.PREF_MAX_NUMBER_OF_INSTRUCTIONS, ICDebugConstants.DEF_NUMBER_OF_INSTRUCTIONS );
		CDebugCorePlugin.getDefault().getPluginPreferences().setDefault( ICDebugConstants.PREF_DEFAULT_VARIABLE_FORMAT, ICDIFormat.NATURAL );
		CDebugCorePlugin.getDefault().getPluginPreferences().setDefault( ICDebugConstants.PREF_DEFAULT_EXPRESSION_FORMAT, ICDIFormat.NATURAL );
		CDebugCorePlugin.getDefault().getPluginPreferences().setDefault( ICDebugConstants.PREF_DEFAULT_REGISTER_FORMAT, ICDIFormat.NATURAL );
	}

	/**
	 * @see DialogPage#dispose()
	 */
	public void dispose()
	{
		super.dispose();
		getPreferenceStore().removePropertyChangeListener( getPropertyChangeListener() );
	}

	/**
	 * Create the view setting preferences composite widget
	 */
	private void createViewSettingPreferences( Composite parent )
	{
		Composite comp = createGroupComposite( parent, 1, CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.Opened_view_default_settings") ); //$NON-NLS-1$
		fPathsButton = createCheckButton( comp, CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.pathsButton") ); //$NON-NLS-1$
		fRefreshRegistersButton = createCheckButton( comp, CDebugUIPlugin.getResourceString( "internal.ui.preferences.CDebugPreferencePage.autoRefreshRegistersButton" ) ); //$NON-NLS-1$
		fRefreshSolibsButton = createCheckButton( comp, CDebugUIPlugin.getResourceString( "internal.ui.preferences.CDebugPreferencePage.autoRefreshSolibsButton" ) ); //$NON-NLS-1$
		Composite formatComposite = ControlFactory.createCompositeEx( comp, 2, 0 );
		((GridLayout)formatComposite.getLayout()).makeColumnsEqualWidth = true;
		fVariableFormatCombo = createComboBox( formatComposite, CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.variableFormatCombo"), fFormatLabels, fFormatLabels[0] ); //$NON-NLS-1$
		fExpressionFormatCombo = createComboBox( formatComposite, CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.expressionFormatCombo"), fFormatLabels, fFormatLabels[0] ); //$NON-NLS-1$
		fRegisterFormatCombo = createComboBox( formatComposite, CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.registerFormatCombo"), fFormatLabels, fFormatLabels[0] ); //$NON-NLS-1$
	}

	/**
	 * Create the disassembly setting preferences composite widget
	 */
	private void createDisassemblySettingPreferences( Composite parent )
	{
		Composite comp = createGroupComposite( parent, 1, CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.Disassembly_options") ); //$NON-NLS-1$
		fAutoDisassemblyButton = createCheckButton( comp, CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.autoDisassemblyButton") ); //$NON-NLS-1$
		createMaxNumberOfInstructionsField( comp );
	}

	private void createMaxNumberOfInstructionsField( Composite parent )
	{
		Composite composite = ControlFactory.createComposite( parent, 2 );
		fMaxNumberOfInstructionsText = new IntegerFieldEditor( ICDebugConstants.PREF_MAX_NUMBER_OF_INSTRUCTIONS,
															   CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.maxNumberOfInstructions"), //$NON-NLS-1$
															   composite,
															   NUMBER_OF_DIGITS );		
		GridData data = (GridData)fMaxNumberOfInstructionsText.getTextControl( composite ).getLayoutData();
		data.horizontalAlignment = GridData.BEGINNING;
		data.widthHint = convertWidthInCharsToPixels( NUMBER_OF_DIGITS + 1 );
		fMaxNumberOfInstructionsText.setPreferencePage( this );
		fMaxNumberOfInstructionsText.setValidateStrategy( StringFieldEditor.VALIDATE_ON_KEY_STROKE );
		fMaxNumberOfInstructionsText.setValidRange( ICDebugConstants.MIN_NUMBER_OF_INSTRUCTIONS, ICDebugConstants.MAX_NUMBER_OF_INSTRUCTIONS );
		String minValue = Integer.toString( ICDebugConstants.MIN_NUMBER_OF_INSTRUCTIONS );
		String maxValue = Integer.toString( ICDebugConstants.MAX_NUMBER_OF_INSTRUCTIONS );
		fMaxNumberOfInstructionsText.setErrorMessage( MessageFormat.format( CDebugUIPlugin.getResourceString("internal.ui.preferences.CDebugPreferencePage.ErrorMaxNumberOfInstructionsRange"), new String[]{ minValue, maxValue } ) ); //$NON-NLS-1$
		fMaxNumberOfInstructionsText.load();
		fMaxNumberOfInstructionsText.setPropertyChangeListener( 
					new IPropertyChangeListener()
						{
							public void propertyChange( PropertyChangeEvent event )
							{
								if ( event.getProperty().equals( FieldEditor.IS_VALID ) )
									setValid( getMaxNumberOfInstructionsText().isValid() );
							}
						} );
	}

	/**
	 * Creates a button with the given label and sets the default 
	 * configuration data.
	 */
	private Button createCheckButton( Composite parent, String label )
	{
		Button button = new Button( parent, SWT.CHECK | SWT.LEFT );
		button.setText( label );
		// FieldEditor GridData
		GridData data = new GridData();
		button.setLayoutData( data );
		return button;
	}

	/**
	 * Creates a button with the given label and sets the default 
	 * configuration data.
	 */
	private Combo createComboBox( Composite parent, String label, String[] items, String selection )
	{
		ControlFactory.createLabel( parent, label );
		Combo combo = ControlFactory.createSelectCombo( parent, items, selection );
		combo.setLayoutData( new GridData() );
		return combo;
	}

	protected void createSpacer( Composite composite, int columnSpan )
	{
		Label label = new Label( composite, SWT.NONE );
		GridData gd = new GridData();
		gd.horizontalSpan = columnSpan;
		label.setLayoutData( gd );
	}

	/**
	 * @see IPreferencePage#performOk()
	 * Also, notifies interested listeners
	 */
	public boolean performOk()
	{
		storeValues();
		if ( getPropertyChangeListener().hasStateChanged() )
		{
			refreshViews();
		}
		CDebugUIPlugin.getDefault().savePluginPreferences();
		CDebugCorePlugin.getDefault().savePluginPreferences();
		return true;
	}

	/**
	 * Refresh the variables and expression views as changes
	 * have occurred that affects these views.
	 */
	private void refreshViews()
	{
		BusyIndicator.showWhile( getShell().getDisplay(), 
								 new Runnable()
									{
										public void run()
										{
											// Refresh interested views
											IWorkbenchWindow[] windows = CDebugUIPlugin.getDefault().getWorkbench().getWorkbenchWindows();
											IWorkbenchPage page = null;
											for ( int i = 0; i < windows.length; i++ )
											{
												page = windows[i].getActivePage();
												if ( page != null )
												{
													refreshViews( page, IDebugUIConstants.ID_EXPRESSION_VIEW );
													refreshViews( page, IDebugUIConstants.ID_VARIABLE_VIEW );
													refreshViews( page, ICDebugUIConstants.ID_REGISTERS_VIEW );
												}
											}
										}
									} );
	}

	/**
	 * Refresh all views in the given workbench page with the given view id
	 */
	protected void refreshViews( IWorkbenchPage page, String viewID )
	{
		IViewPart part = page.findView( viewID );
		if ( part != null )
		{
			IDebugView adapter = (IDebugView)part.getAdapter( IDebugView.class );
			if ( adapter != null )
			{
				Viewer viewer = adapter.getViewer();
				if ( viewer instanceof StructuredViewer )
				{
					((StructuredViewer)viewer).refresh();
				}
			}
		}
	}

	/**
	 * Store the preference values based on the state of the component widgets
	 */
	private void storeValues()
	{
		IPreferenceStore store = getPreferenceStore();
		store.setValue( ICDebugPreferenceConstants.PREF_SHOW_FULL_PATHS, fPathsButton.getSelection() );
		CDebugCorePlugin.getDefault().getPluginPreferences().setValue( ICDebugConstants.PREF_REGISTERS_AUTO_REFRESH, fRefreshRegistersButton.getSelection() );
		CDebugCorePlugin.getDefault().getPluginPreferences().setValue( ICDebugConstants.PREF_SHARED_LIBRARIES_AUTO_REFRESH, fRefreshSolibsButton.getSelection() );
		CDebugCorePlugin.getDefault().getPluginPreferences().setValue( ICDebugConstants.PREF_AUTO_DISASSEMBLY, fAutoDisassemblyButton.getSelection() );
		CDebugCorePlugin.getDefault().getPluginPreferences().setValue( ICDebugConstants.PREF_MAX_NUMBER_OF_INSTRUCTIONS, getMaxNumberOfInstructionsText().getIntValue() );
		CDebugCorePlugin.getDefault().getPluginPreferences().setValue( ICDebugConstants.PREF_DEFAULT_VARIABLE_FORMAT, getFormatId( fVariableFormatCombo.getSelectionIndex() ) );
		CDebugCorePlugin.getDefault().getPluginPreferences().setValue( ICDebugConstants.PREF_DEFAULT_EXPRESSION_FORMAT, getFormatId( fExpressionFormatCombo.getSelectionIndex() ) );
		CDebugCorePlugin.getDefault().getPluginPreferences().setValue( ICDebugConstants.PREF_DEFAULT_REGISTER_FORMAT, getFormatId( fRegisterFormatCombo.getSelectionIndex() ) );
	}

	/**
	 * Sets the default preferences.
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults()
	{
		setDefaultValues();
		super.performDefaults();
	}

	private void setDefaultValues()
	{
		IPreferenceStore store = getPreferenceStore();
		fPathsButton.setSelection( store.getDefaultBoolean( ICDebugPreferenceConstants.PREF_SHOW_FULL_PATHS ) );
		fRefreshRegistersButton.setSelection( CDebugCorePlugin.getDefault().getPluginPreferences().getDefaultBoolean( ICDebugConstants.PREF_REGISTERS_AUTO_REFRESH ) );
		fRefreshSolibsButton.setSelection( CDebugCorePlugin.getDefault().getPluginPreferences().getDefaultBoolean( ICDebugConstants.PREF_SHARED_LIBRARIES_AUTO_REFRESH ) );
		fAutoDisassemblyButton.setSelection( CDebugCorePlugin.getDefault().getPluginPreferences().getDefaultBoolean( ICDebugConstants.PREF_AUTO_DISASSEMBLY ) );
		getMaxNumberOfInstructionsText().setStringValue( new Integer( CDebugCorePlugin.getDefault().getPluginPreferences().getDefaultInt( ICDebugConstants.PREF_MAX_NUMBER_OF_INSTRUCTIONS ) ).toString() );
		fVariableFormatCombo.select( getFormatIndex( CDebugCorePlugin.getDefault().getPluginPreferences().getDefaultInt( ICDebugConstants.PREF_DEFAULT_VARIABLE_FORMAT ) ) );
		fExpressionFormatCombo.select( getFormatIndex( CDebugCorePlugin.getDefault().getPluginPreferences().getDefaultInt( ICDebugConstants.PREF_DEFAULT_EXPRESSION_FORMAT ) ) );
		fRegisterFormatCombo.select( getFormatIndex( CDebugCorePlugin.getDefault().getPluginPreferences().getDefaultInt( ICDebugConstants.PREF_DEFAULT_REGISTER_FORMAT ) ) );
	}
	
	private static int getFormatId( int index )
	{
		return ( index >= 0 && index < fFormatIds.length ) ? fFormatIds[index] : fFormatIds[0];
	}

	private static int getFormatIndex( int id )
	{
		for ( int i = 0; i < fFormatIds.length; ++i )
			if ( fFormatIds[i] == id )
				return i;
		return -1;
	}

	protected IntegerFieldEditor getMaxNumberOfInstructionsText()
	{
		return fMaxNumberOfInstructionsText;
	}
}
