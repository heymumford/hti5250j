# SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
#
# SPDX-License-Identifier: GPL-2.0-or-later





from java.lang import *
hostOs = System.getProperty('os.name')
cmd = 'unknown'
if (hostOs.startswith('Mac OS X') ):
	cmd = 'open /Applications/Calculator.app'
else:
	cmd = 'calc.exe'
print cmd
Runtime.getRuntime().exec(cmd)
