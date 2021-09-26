package com.example.boatcaptain;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;

public class filedata {

	public String getFilename() {
		return m_sFilename;
	}

	public filedata(String sFilename) {
		int MARK_LIMIT = 999999999;// set to arbitrarily high value
		m_bFileOpenError = false;
		m_context = null;
		m_sFilename = sFilename;
		// Get the text file
		m_file = new File(sFilename);

		// Read text from file
		if (m_file != null) {
			try {
				m_br = new BufferedReader(new FileReader(m_file));
			} catch (IOException e) {
				m_bFileOpenError = true;
			}
		} else
			m_bFileOpenError = true;
		if (!m_bFileOpenError) {// mark start of buffer
			try {
				m_br.mark(MARK_LIMIT);
			} catch (IOException e) {
				m_bFileOpenError = true;
			}
		}
	}

	public filedata(byte[] buffer, int nSize) {
		int MARK_LIMIT = 999999999;// set to arbitrarily high value
		m_bFileOpenError = false;
		m_context = null;
		m_sFilename = "";
		ByteArrayInputStream byteInput = new ByteArrayInputStream(buffer, 0,
				nSize);
		InputStreamReader isReader = new InputStreamReader(byteInput);
		m_br = new BufferedReader(isReader);
		try {
			m_br.mark(MARK_LIMIT);
		} catch (IOException e) {
			m_bFileOpenError = true;
		}

	}

	// private data
	private Context m_context;
	private boolean m_bFileOpenError;
	private BufferedReader m_br;
	private File m_file;
	private String m_sFilename;

	public String getString(String sCategory, String sField) {
		String sRetval = FindString(sCategory, sField);
		if (sRetval == null)
			return "";
		return sRetval;
	}

	public double getDouble(String sCategory, String sField) {
		String sVal = FindString(sCategory, sField);
		if (sVal == null || sVal == "")
			return 0.0;
		double dRetval = Double.parseDouble(sVal);
		return dRetval;
	}

	public double[] getDouble(String sCategory, String sField, int numItems) {
		if (numItems <= 0)
			return null;
		double[] dRetval = new double[numItems];
		String sVal = FindString(sCategory, sField);
		if (sVal == null || sVal == "")
			return dRetval;
		String sDelims = "[ ]+";
		String[] sDoubleVals = sVal.split(sDelims);
		int nNumVals = sDoubleVals.length;
		if (nNumVals <= 0)
			return dRetval;
		for (int i = 0; i < nNumVals; i++) {
			dRetval[i] = Double.parseDouble(sDoubleVals[i]);
		}
		return dRetval;
	}
	

	public double[][] get2DDouble(String sCategory, String sField,
			int nNumSegments, int nNumAxes) {
		if (nNumSegments <= 0 || nNumAxes <= 0)
			return null;
		double[][] dRetval = new double[nNumSegments][nNumAxes];

		try {
			String sVal = FindString(sCategory, sField);
			if (sVal.equals("")) {
				return dRetval;
			}
			String sDelims = "[ ]+";
			String[] sDoubleVals = sVal.split(sDelims);

			int nNumVals = sDoubleVals.length;
			if (nNumVals <= 0)
				return dRetval;

			for (int i = 0; i < nNumSegments; i++) {
				for (int j = 0; j < nNumAxes; j++) {
					int k = i * nNumAxes + j;
					if (k >= nNumVals) {
						dRetval[i][j] = 0.0;
					} else {
						dRetval[i][j] = Double.parseDouble(sDoubleVals[k]);
					}
				}
			}
		} catch (Exception e) {
			if (this.m_context != null) {
				String sError = String.format("Error in Get2DDouble: %s",
						e.toString());
				SimpleMessage.msbox("Error", sError, m_context);
			}
		}
		return dRetval;
	}

	public void closeDataFile() {
		if (m_br != null) {
			try {
				m_br.close();
			} catch (IOException e) {
				// do nothing
			}
		}
	}

	public int getInteger(String sCategory, String sField) {
		String sVal = FindString(sCategory, sField);
		if (sVal == null || sVal == "")
			return 0;
		int nRetval = Integer.parseInt(sVal);
		return nRetval;
	}

	public int[] getInteger(String sCategory, String sField, int nNumItems) {
		if (nNumItems <= 0)
			return null;
		int[] nRetval = new int[nNumItems];
		String sVals = FindString(sCategory, sField);
		if (sVals == null || sVals == "") {
			return nRetval;
		}
		String sDelims = "[ ]+";
		String[] sIntVals = sVals.split(sDelims);

		int nNumVals = sIntVals.length;
		if (nNumVals <= 0)
			return nRetval;

		for (int i = 0; i < nNumVals; i++) {
			nRetval[i] = Integer.parseInt(sIntVals[i]);
		}
		return nRetval;
	}

	public void SetContext(Context cText) {
		m_context = cText;
	}

	public String FindString(String sCategory, String sField) {
		// left off here - does not work if sField does not exist - can
		String sRetval = "";
		if (m_bFileOpenError)
			return sRetval;
		if (m_br == null)
			return sRetval;
		
		String sLine = null;
		try {
			try {
				m_br.reset();
			} catch (IOException e) {
				return sRetval;
			}
			try {
				sLine = m_br.readLine();
			} catch (IOException e) {
				return sRetval;
			}
			while (sLine != null) {
				if (sLine.length() > 0) {
					if (sLine.startsWith(sCategory)) {
						// read more lines until sField is found
						try {
							sLine = m_br.readLine();
						} catch (IOException e) {
							return sRetval;
						}
						while (sLine != null) {
							if (sLine.length() > 0) {
								if (sLine.startsWith("[")) {//came to the next category
									return sRetval;
								}
								if (sLine.startsWith(sField)) {
									int nStartIndex = sField.length();
									int nLineLength = sLine.length();
									while (nStartIndex < nLineLength) {
										if (sLine.charAt(nStartIndex) == ' '
												|| sLine.charAt(nStartIndex) == '\t') {
											nStartIndex++;
										} else
											break;
									}
									if (nStartIndex < nLineLength) {
										sRetval = sLine.substring(nStartIndex,
												nLineLength);
									}
									return sRetval;
								}
							}
							try {
								sLine = m_br.readLine();
							} catch (IOException e) {
								return sRetval;
							}
						}
						break;
					}
				}
				try {
					sLine = m_br.readLine();// read next line
				} catch (Exception e) {
					return sRetval;
				}
			}
		} catch (Exception e) {
			if (m_context != null) {
				String sError = String.format("Error finding string: %s",
						e.toString());
				SimpleMessage.msbox("Error", sError, m_context);
			}
			return sRetval;
		}
		return sRetval;
	}
}
