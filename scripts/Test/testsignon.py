# SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
#
# SPDX-License-Identifier: GPL-2.0-or-later





print "--------------- tn5250j test send keys script start ------------"

screen = _session.getScreen()

screen.sendKeys("userid[fldext]password[fldext][enter][enter][enter][enter]")

print "---------------- tn5250j test send keys script end -------------"
