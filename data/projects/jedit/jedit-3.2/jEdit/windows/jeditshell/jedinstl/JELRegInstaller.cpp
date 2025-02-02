/*
 * JELRegInstaller.cpp - part of jEditLauncher package
 * Copyright (C) 2001 John Gellene
 * jgellene@nyc.rr.com
 *
 * Notwithstanding the terms of the General Public License, the author grants
 * permission to compile and link object code generated by the compilation of
 * this program with object code and libraries that are not subject to the
 * GNU General Public License, provided that the executable output of such
 * compilation shall be distributed with source code on substantially the
 * same basis as the jEditLauncher package of which this program is a part.
 * By way of example, a distribution would satisfy this condition if it
 * included a working makefile for any freely available make utility that
 * runs on the Windows family of operating systems. This condition does not
 * require a licensee of this software to distribute any proprietary software
 * (including header files and libraries) that is licensed under terms
 * prohibiting redistribution to third parties.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * $Id: JELRegInstaller.cpp,v 1.5 2001/08/28 19:36:43 jgellene Exp $
 */

#include "stdafx.h"
#include <string.h>
#include "InstallData.h"
#include "CtxMenuDlg.h"
#include "StringPtr.h"
#include "JELRegInstaller.h"
#include <assert.h>

#include <stdlib.h> // for itoa in debug

/* Implementation of JELRegistryInstaller */

JELRegistryInstaller::JELRegistryInstaller(const InstallData *ptrData,
										   Installer *ptrOwner)
	: pData(ptrData), pOwner(ptrOwner) {}

JELRegistryInstaller::~JELRegistryInstaller() {}

HRESULT JELRegistryInstaller::Install()
{
	HRESULT hr;

	// register proxy/stub DLL
	CString strPath(pData->strInstallDir);
	strPath += _T("\\jeservps.dll");
	if(FAILED(hr = RegisterDLL(strPath, TRUE)))
		return hr;

	// registration of unlaunch for "Add/Remove Programs" applet
	// other data added by COM server
	RegisterUninstall();

	// register COM server
	strPath = pData->strInstallDir;
	strPath += _T("\\jeditsrv.exe");
	if(FAILED(hr = RegisterEXE(strPath)))
	{
		// TODO: try to restore former setting
		return hr;
	}

	// register the context menu handler
	if(FAILED(hr = RegisterDLL(pData->strInstallPath, TRUE)))
		return hr;

	// if we are still using the temporary file name, fix the
	// registration entry so it will be correct after rebooting
	if(pData->bUsingTempFileName)
	{
		if(FAILED(hr = CorrectTempCtxReg()))
			return hr;
	}

	// tell parent object to update active version in InstallData
	DWORD dwMsg = (MsgInstall << 16) | pData->nIndexSubjectVer;
	pOwner->SendMessage((LPVOID)&dwMsg);

	RegisterPrimaryVersion();
	RegisterCmdLineParameters();
	return hr;
}

HRESULT JELRegistryInstaller::RegisterDLL(LPCTSTR szPath, BOOL bRegister)
{
	FARPROC proc = 0;
	HRESULT hr;
	LPCTSTR szProc = bRegister ? _T("DllRegisterServer") :
		_T("DllUnregisterServer");
	HMODULE hModule = ::LoadLibrary(szPath);
	if(hModule == 0 || (proc = GetProcAddress(hModule, szProc)) == 0
		|| S_OK != (proc)())
	{
		hr = E_FAIL;
	}
	else
	{
		hr = S_OK;
	}
	if(hModule != 0)
		FreeLibrary(hModule);
	return hr;
}

HRESULT JELRegistryInstaller::RegisterEXE(LPCTSTR szPath, BOOL bRegister)
{
	CString strCmdLine(szPath);
	strCmdLine += (bRegister ? _T(" /RegServer") : _T(" /UnregServer"));
	CStringBuf<> bufCmdLine(strCmdLine);
	STARTUPINFO si;
	::ZeroMemory(&si, sizeof(si));
	PROCESS_INFORMATION pi;
	BOOL bReturn =  CreateProcess(0, bufCmdLine, 0, 0, 0, 0, 0, 0, &si, &pi);
	if(!bReturn)
		DWORD swError = GetLastError();
	return bReturn ? S_OK : E_FAIL;
}

HRESULT JELRegistryInstaller::CorrectTempCtxReg()
{
	HKEY hKey = 0;
	CString strClassKeyPath((LPCSTR)IDS_REG_CTX_SERVER_KEY);
	int nResult = RegOpenKeyEx(HKEY_CLASSES_ROOT,
		strClassKeyPath, 0, KEY_SET_VALUE, &hKey);
	if(nResult == ERROR_SUCCESS)
	{
		nResult = RegSetValueEx(hKey, 0, 0, REG_SZ,
			(const LPBYTE)(LPCTSTR)pData->strInstallFinalPath,
			InstallData::GetBufferByteLen(pData->strInstallFinalPath));
	}
	RegCloseKey(hKey);
	return nResult == ERROR_SUCCESS ? S_OK : E_FAIL;
}


HRESULT JELRegistryInstaller::RegisterUninstall()
{
	HKEY hKey;
	CString strUninstallKey((LPCSTR)IDS_REG_UNINSTALL_KEY);
	strUninstallKey += pData->strInstallVersion;
	::OutputDebugString(strUninstallKey);
	int nResult = RegCreateKeyEx(HKEY_CURRENT_USER,
		strUninstallKey, 0, 0, REG_OPTION_NON_VOLATILE,
		KEY_WRITE, 0, &hKey, 0);
	if(nResult == ERROR_SUCCESS)
	{
		::OutputDebugString("Found key\n");
		CString strUninstall(pData->strInstallDir);
		strUninstall += "\\unlaunch.exe";
		nResult = RegSetValueEx(hKey, "UninstallString", 0, REG_SZ,
			(const LPBYTE)(LPCTSTR)strUninstall,
			InstallData::GetBufferByteLen(strUninstall));
		OutputDebugString(nResult == ERROR_SUCCESS ?
			"UninstallString OK" : "UninstallString error");
		CString strInstall(pData->strInstallDir);
		strInstall += "\\jedit.exe /i ";
		strInstall += pData->strJavaHome;
		nResult = RegSetValueEx(hKey, "InstallPath", 0, REG_SZ,
			(const LPBYTE)(LPCTSTR)strInstall,
			InstallData::GetBufferByteLen(strInstall));
		OutputDebugString(nResult == ERROR_SUCCESS ?
			"InstallPath OK" : "InstallPath error");
		CString strDisplayIcon(pData->strInstallDir);
		strDisplayIcon += "\\jedit.exe, 0";
		nResult = RegSetValueEx(hKey, "DisplayIcon", 0, REG_SZ,
			(const LPBYTE)(LPCTSTR)strDisplayIcon,
			InstallData::GetBufferByteLen(strDisplayIcon));
		OutputDebugString(nResult == ERROR_SUCCESS ?
			"DisplayIcon OK" : "DisplayIcon error");
	}
	else
		::OutputDebugString("Could not find key\n");
	RegCloseKey(hKey);
	return (nResult == ERROR_SUCCESS ? S_OK : E_FAIL);
}

HRESULT JELRegistryInstaller::RegisterPrimaryVersion()
{
	if(pData->nInstalledVersions == 0)
		return S_FALSE;
	int nIndexPrimaryVer = pData->nIndexSubjectVer;
	if(pData->nInstalledVersions > 1)
	{
		CChooseCtxMenuDlg dlg(pData);
		dlg.DoModal();
		nIndexPrimaryVer = dlg.GetCtxVersionIndex();
	}
	DWORD dwMessage = (MsgChangePrimary << 16) | nIndexPrimaryVer;
	pOwner->SendMessage((LPVOID)&dwMessage);

	// register scripting object and designated context menu handler
	// get default value for GUID
	// get ProgID under GUID
	// default value becomes default value for class 'JEdit.JEditLauncher'
	// CLSID is GUID
	// CurVer is ProgID

	CString strClassKey(_T("CLSID\\"));
	CString& strGUID = pData->arGUID[nIndexPrimaryVer];
	strClassKey += strGUID;
	CString strClassName;
	CString strCurVer;
	CString strCurVerDir;
	HKEY hKey;
	int nResult;
	int nCounter = 20;
	// waiting for possible registration of new server in separate thread
	do
	{
		Sleep(50);
		nResult = RegOpenKeyEx(HKEY_CLASSES_ROOT, strClassKey, 0,
			KEY_READ | KEY_QUERY_VALUE, &hKey);
	} while(nResult != ERROR_SUCCESS && --nCounter != 0);
	if(nResult == ERROR_SUCCESS)
	{
		CStringBuf<>pClassBuf(strClassName);
		DWORD dwLength = pClassBuf.Size();
		RegQueryValueEx(hKey, 0, 0, 0, (LPBYTE)pClassBuf, &dwLength);
		HKEY hSubkey;
		nResult = RegOpenKeyEx(hKey, _T("ProgID"), 0, KEY_READ, &hSubkey);
		if(nResult == ERROR_SUCCESS)
		{
			CStringBuf<>pCurVerBuf(strCurVer);
			dwLength = pCurVerBuf.Size();
			RegQueryValueEx(hSubkey, 0, 0, 0, (LPBYTE)pCurVerBuf, &dwLength);
		}
		RegCloseKey(hSubkey);
		nResult = RegOpenKeyEx(hKey, _T("LocalServer32"), 0, KEY_READ, &hSubkey);
		if(nResult == ERROR_SUCCESS)
		{
			CStringBuf<>pCurVerDirBuf(strCurVerDir);
			dwLength = pCurVerDirBuf.Size();
			RegQueryValueEx(hSubkey, 0, 0, 0, (LPBYTE)pCurVerDirBuf, &dwLength);
			InstallData::ShortenToDirectory(pCurVerDirBuf);
		}
		RegCloseKey(hSubkey);
	}
	RegCloseKey(hKey);
	nResult = RegCreateKeyEx(HKEY_CLASSES_ROOT, _T("JEdit.JEditLauncher"), 0, 0, 0,
		KEY_ALL_ACCESS, 0, &hKey, 0);
	if(nResult == ERROR_SUCCESS)
	{
		RegSetValueEx(hKey, 0, 0, REG_SZ, (LPBYTE)(LPCTSTR)strClassName,
			InstallData::GetBufferByteLen(strClassName));
		HKEY hSubkey;
		nResult = RegCreateKeyEx(hKey, _T("CLSID"), 0, 0, 0, KEY_ALL_ACCESS,
			0, &hSubkey, 0);
		if(nResult == ERROR_SUCCESS)
		{
			RegSetValueEx(hSubkey, 0, 0, REG_SZ, (LPBYTE)(LPCTSTR)strGUID,
				InstallData::GetBufferByteLen(strGUID));
		}
		RegCloseKey(hSubkey);
		nResult = RegCreateKeyEx(hKey, _T("CurVer"), 0, 0, 0, KEY_ALL_ACCESS,
			0, &hSubkey, 0);
		if(nResult == ERROR_SUCCESS)
		{
			RegSetValueEx(hSubkey, 0, 0, REG_SZ, (LPBYTE)(LPCTSTR)strCurVer,
				InstallData::GetBufferByteLen(strCurVer));
		}
		RegCloseKey(hSubkey);
	}
	RegCloseKey(hKey);
	if(nResult != ERROR_SUCCESS)
		return E_FAIL;
	if(nIndexPrimaryVer != pData->nIndexSubjectVer)
	{
		CString strNewPrimaryPath(strCurVerDir);
		strNewPrimaryPath += _T("\\jeshlstb.dll");
		RegisterDLL(strNewPrimaryPath, TRUE);
		strNewPrimaryPath = strCurVerDir;
		strNewPrimaryPath += _T("\\jeservps.dll");
		RegisterDLL(strNewPrimaryPath, TRUE);
		strNewPrimaryPath = strCurVerDir;
		strNewPrimaryPath += _T("\\jeditsrv.exe");
		RegisterEXE(strNewPrimaryPath, TRUE);
	}
	return S_OK;
}



HRESULT JELRegistryInstaller::RegisterCmdLineParameters()
{
	HKEY hLauncherKey, hVersionKey;
	DWORD dwDisp;
	int nResult;
	DWORD dwValueSize;
	TCHAR szBuf[MAX_PATH];
	TCHAR* arszValues[] = { _T("Java Executable"),
							_T("Java Options"),
							_T("jEdit Target"),
							_T("jEdit Options"),
							_T("jEdit Server File"),
							_T("jEdit Working Directory"),
							_T("Launcher GUID"),
							_T("Installation Directory") };
/*
	enum { JavaExec, JavaOptions, jEditTarget, jEditOptions,
			Server, WorkingDir, GUID, InstallDir };
*/
	// ask if parameters should be retained from last installation
	bool bUseOldParameters = false;
	if(pData->nIndexCurrentVer != -1
		&& IDYES == MessageBox(0,
			"Do you wish to retain command line parameters from your existing installation of jEditLauncher?",
			"jEditLauncher", MB_ICONQUESTION | MB_YESNO))
	{
		bUseOldParameters = true;
	}

	CString strLauncherParamKeyPath((LPCSTR)IDS_REG_LAUNCHER_PARAM_KEY);
	RegCreateKeyEx(HKEY_CURRENT_USER, strLauncherParamKeyPath, 0, 0, 0,
			KEY_ALL_ACCESS, 0, &hLauncherKey, 0);
	CString strVersion(pData->arVersionNames[pData->nIndexSubjectVer]);
	RegCreateKeyEx(hLauncherKey, strVersion, 0, 0, 0,
		KEY_ALL_ACCESS, 0, &hVersionKey, &dwDisp);

	if(!bUseOldParameters)
	{
		// "Java Executable"
		nResult = RegQueryValueEx(hVersionKey, arszValues[0], 0, 0, 0, &dwValueSize);
		if(nResult != ERROR_SUCCESS || dwValueSize == 0)
		{
			CString strJavaExec(pData->strJavaHome);
			strJavaExec += _T("\\javaw.exe");
			RegSetValueEx(hVersionKey, arszValues[0], 0,
				REG_SZ, (LPBYTE)(LPCTSTR)strJavaExec,
				InstallData::GetBufferByteLen(strJavaExec));
		}

		// "Java Options"
		nResult = RegQueryValueEx(hVersionKey, arszValues[1], 0, 0, (LPBYTE)szBuf, &dwValueSize);
		if(nResult != ERROR_SUCCESS || dwValueSize == 0)
		{
			CString strJavaOptions;
			strJavaOptions.Format(IDS_REG_JAVAPARAM_DEFAULT,
				pData->arPathNames[pData->nIndexSubjectVer]);
				RegSetValueEx(hVersionKey, arszValues[1], 0,
				REG_SZ, (LPBYTE)(LPCTSTR)strJavaOptions,
				InstallData::GetBufferByteLen(strJavaOptions));
		}
		else
		{
			UpdateJarParameter(szBuf);
			RegSetValueEx(hVersionKey, arszValues[1], 0,
				REG_SZ, (LPBYTE)szBuf,
				(_tcslen(szBuf) + 1) * sizeof(TCHAR));
		}
	}

	// "jEdit Target" -- must always be set
	nResult = RegQueryValueEx(hVersionKey, arszValues[2], 0, 0, (LPBYTE)szBuf, &dwValueSize);
	if(nResult != ERROR_SUCCESS || dwValueSize == 0)
	{
		CString strJEditTarget(_T("org.gjt.sp.jedit.jEdit"));
		RegSetValueEx(hVersionKey, arszValues[2], 0,
			REG_SZ, (LPBYTE)(LPCTSTR)strJEditTarget,
			InstallData::GetBufferByteLen(strJEditTarget));
	}
	else
	{
		UpdateJarParameter(szBuf);
		RegSetValueEx(hVersionKey, arszValues[2], 0,
			REG_SZ, (LPBYTE)szBuf,
			(_tcslen(szBuf) + 1) * sizeof(TCHAR));
	}


	if(!bUseOldParameters)
	{
		// "jEdit Options"
		nResult = RegQueryValueEx(hVersionKey, arszValues[3], 0, 0, (LPBYTE)szBuf, &dwValueSize);
		if(nResult != ERROR_SUCCESS || dwValueSize == 0)
		{
			szBuf[0] = 0;
			nResult = RegSetValueEx(hVersionKey, arszValues[3], 0,
			REG_SZ, (LPBYTE)szBuf, sizeof(TCHAR));
			::OutputDebugString( nResult == ERROR_SUCCESS ? "OK" : "Error");
		}

		// "jEdit Server File"
		nResult = RegQueryValueEx(hVersionKey, arszValues[4], 0, 0, (LPBYTE)szBuf, &dwValueSize);
		if(nResult != ERROR_SUCCESS || dwValueSize == 0)
		{
			szBuf[0] = 0;
			RegSetValueEx(hVersionKey, arszValues[4], 0,
			REG_SZ, (LPBYTE)szBuf, sizeof(TCHAR));
		}
	}

	// "jEdit Working Directory"
	// always change working directory, so you can't get
	// an orphan entry following an uninstall of another version

	CString strWorkingDir(pData->arPathNames[pData->nIndexSubjectVer]);
	RegSetValueEx(hVersionKey, arszValues[5], 0,
		REG_SZ, (LPBYTE)(LPCTSTR)strWorkingDir,
		InstallData::GetBufferByteLen(strWorkingDir));

	// "Launcher GUID"
	RegSetValueEx(hVersionKey, arszValues[6], 0,
		REG_SZ, (LPBYTE)(LPCTSTR)pData->strInstallGUID,
		InstallData::GetBufferByteLen(pData->strInstallGUID));

	// "Installation Dir"
	RegSetValueEx(hVersionKey, arszValues[7], 0,
		REG_SZ, (LPBYTE)(LPCTSTR)pData->strInstallDir,
		InstallData::GetBufferByteLen(pData->strInstallDir));


	RegCloseKey(hVersionKey);
	RegCloseKey(hLauncherKey);
	return S_OK;
}

// if "jEdit.jar" appears in the parameter,
// get the associated directory and change to the
// current install directory
void JELRegistryInstaller::UpdateJarParameter(LPTSTR lpszParam)
{
	CString strLCParam(lpszParam);
	strLCParam.MakeLower();
	const TCHAR *pJar = lpszParam + strLCParam.Find(_T("jedit.jar"));
	if(pJar <= lpszParam) return;
	bool quoted = false;
	const TCHAR *pStart = lpszParam;
	const TCHAR *p;
	for(p = lpszParam; p < pJar; ++p)
	{
		switch (*p)
		{
			case _T(';'): pStart = p; break;
			case _T(' '): if(!quoted) pStart = p; break;
			case _T('\"'): quoted = !quoted; if(quoted) pStart = p; break;
			default: break;
		}
	}
	// now pStart points to the beginning of the jedit.jar path,
	// or a leading space, quote or semicolon; next we trim
	bool bContinue = true;
	for(p = pStart; bContinue && p < pJar; ++p)
	{
		switch(*p)
		{
			case _T(';'):
			case _T(' '):
			case _T('\"'): pStart = p; break;
			default: bContinue = false; break;
		}
	}
	// now we replace from pStart to pJar with the new install dir
	char szNewParam[MAX_PATH];
	ZeroMemory(szNewParam, MAX_PATH);
	lstrcpyn(szNewParam, lpszParam, pStart - lpszParam);
	CString strPath(pData->strInstallPath.Left(pData->strInstallPath.GetLength() - 12));
	lstrcat(szNewParam, strPath);
	lstrcat(szNewParam, pJar);
	lstrcpy(lpszParam, szNewParam);
}


HRESULT JELRegistryInstaller::Uninstall()
{
	UnregisterCmdLineParameters();
	CString strDir(pData->arPathNames[pData->nIndexSubjectVer]);

	// unregister proxy/stub DLL
	CString strPath(strDir);
	strPath += _T("\\jeservps.dll");
	RegisterDLL(strPath, FALSE);

	// clean up proxy/stub;  this is interface GUID
	CString strPxStubCLSID(_T("CLSID\\{E53269FA-8A5C-42B0-B3BC-82254F4FCED4}"));
	RegDeleteKey(HKEY_CLASSES_ROOT, strPxStubCLSID + _T("\\InProcServer32"));
	RegDeleteKey(HKEY_CLASSES_ROOT, strPxStubCLSID);

	// unregister COM Server
	strPath = strDir;
	strPath += _T("\\jeditsrv.exe");
	RegisterEXE(strPath, FALSE);

	CheckUnregisterCtxMenu();


	// send message to application object to adjust number
	// of installed versions
	DWORD dwMessage = (DWORD)pData->nIndexSubjectVer;
	pOwner->SendMessage((LPVOID)&dwMessage);

	// delete HKEY_CLASSES_ROOT\JEdit.JEditLauncher  and interface key if nothing is left
	if(pData->nInstalledVersions == 0)
	{
		HKEY hLauncherKey;
		if(ERROR_SUCCESS == RegOpenKeyEx(HKEY_CLASSES_ROOT, _T("JEdit.JEditLauncher"),
			0, KEY_ALL_ACCESS, &hLauncherKey))
		{
			RegDeleteKey(hLauncherKey, _T("CLSID"));
			RegDeleteKey(hLauncherKey, _T("CurVer"));
			RegCloseKey(hLauncherKey);
			RegDeleteKey(HKEY_CLASSES_ROOT, _T("JEdit.JEditLauncher"));
		}
		CString strInterfaceKey(_T("Interface\\{E53269FA-8A5C-42B0-B3BC-82254F4FCED4}"));
		if(ERROR_SUCCESS == RegOpenKeyEx(HKEY_CLASSES_ROOT, strInterfaceKey,
			0, KEY_ALL_ACCESS, &hLauncherKey))
		{
			RegDeleteKey(hLauncherKey, _T("NumMethods"));
			RegDeleteKey(hLauncherKey, _T("ProxyStubClsid"));
			RegDeleteKey(hLauncherKey, _T("ProxyStubClsid32"));
			RegDeleteKey(hLauncherKey, _T("TypeLib"));
			RegCloseKey(hLauncherKey);
			RegDeleteKey(HKEY_CLASSES_ROOT, strInterfaceKey);
		}
	}
	// otherwise set a new primary version
	else RegisterPrimaryVersion();

	return S_OK;
}

HRESULT JELRegistryInstaller::UnregisterCmdLineParameters()
{
	HKEY hKey;
	// delete command line parameters and any empty parent keys
	CString strLauncherParamKey((LPCSTR)IDS_REG_LAUNCHER_PARAM_KEY);
	int nResult = RegOpenKeyEx(HKEY_CURRENT_USER, strLauncherParamKey,
		0, KEY_ALL_ACCESS,  &hKey);
	if(nResult == ERROR_SUCCESS)
	{
		CString strVersion(pData->arVersionNames[pData->nIndexSubjectVer]);
		RegDeleteKey(hKey, strVersion);
		DWORD dwSubkeys;
		RegQueryInfoKey(hKey, 0, 0, 0, &dwSubkeys, 0, 0, 0, 0, 0, 0, 0);
		if(dwSubkeys == 0)
		{
			RegCloseKey(hKey);
			if(ERROR_SUCCESS == RegOpenKeyEx(HKEY_CURRENT_USER,
				_T("Software\\www.jedit.org"), 0, KEY_ALL_ACCESS, &hKey))
			{
				RegDeleteKey(hKey, _T("jEditLauncher"));
				RegQueryInfoKey(hKey, 0, 0, 0, &dwSubkeys, 0, 0, 0, 0, 0, 0, 0);
				if(dwSubkeys == 0)
				{
					RegCloseKey(hKey);
					if(ERROR_SUCCESS == RegOpenKeyEx(HKEY_CURRENT_USER,
						_T("Software"), 0, KEY_ALL_ACCESS, &hKey))
					{
						RegDeleteKey(hKey, _T("www.jedit.org"));
					}
				}
			}
		}
	}
	RegCloseKey(hKey);
	return S_OK;
}

// Unregisters ctx menu handler if the current version is installed
HRESULT JELRegistryInstaller::CheckUnregisterCtxMenu()
{
	// first get the CLSID of the installed handler
	CString strCtxMenuKey((LPCSTR)IDS_REG_CTXMENU_KEY);
	CString strCurCtxMenuCLSID;
	HKEY hCtxMenuKey;
	int nResult = RegOpenKeyEx(HKEY_CLASSES_ROOT, strCtxMenuKey, 0,
		KEY_ALL_ACCESS, &hCtxMenuKey);
	if(nResult == ERROR_SUCCESS)
	{
		CStringBuf<>buf(strCurCtxMenuCLSID);
		DWORD dwlength = buf * sizeof(TCHAR);
		nResult = RegQueryValueEx(hCtxMenuKey, 0, 0, 0,
			(LPBYTE)buf, &dwlength);
	}
	RegCloseKey(hCtxMenuKey);

	// now get the path to the handler module from the CLSID
	CString strCurCtxMenuPath;
	CString strServerKey(_T("CLSID\\"));
	strServerKey += strCurCtxMenuCLSID;
	strServerKey += _T("\\InprocServer32");
	nResult = RegOpenKeyEx(HKEY_CLASSES_ROOT, strServerKey, 0,
		KEY_ALL_ACCESS, &hCtxMenuKey);
	if(nResult == ERROR_SUCCESS)
	{
		CStringBuf<>buf(strCurCtxMenuPath);
		DWORD dwlength = buf * sizeof(TCHAR);
		nResult = RegQueryValueEx(hCtxMenuKey, 0, 0, 0,
			(LPBYTE)buf, &dwlength);
	}
	RegCloseKey(hCtxMenuKey);


	// now compare the installed handler's path to
	// the path in this version and delete the key if they match
	CString strPath(pData->arPathNames[pData->nIndexSubjectVer]);
	strPath += _T("\\jeshlstb.dll");
	TCHAR szInstalledPath[MAX_PATH], szCurPath[MAX_PATH];
	GetShortPathName(strCurCtxMenuPath, szInstalledPath, MAX_PATH);
	GetShortPathName(strPath, szCurPath, MAX_PATH);
	HRESULT hr = S_OK;
	if(_tcsicmp(szInstalledPath, szCurPath) == 0)
	{
		RegDeleteKey(HKEY_CLASSES_ROOT, strCtxMenuKey);
		RegisterDLL(szCurPath, FALSE);
	}
	else hr = S_FALSE;
	return hr;
}

HRESULT JELRegistryInstaller::SendMessage(LPVOID p)
{
	p;
	return E_NOTIMPL;
}


