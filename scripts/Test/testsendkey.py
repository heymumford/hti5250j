# SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
#
# SPDX-License-Identifier: GPL-2.0-or-later





print "--------------- tn5250j test send keys script start ------------"

print "--------------- session ------------"

print _session
screen = _session.getScreen()

print "--------------- screen ------------"
print screen
screen.sendKeys("[enter]")

print "---------------- tn5250j test send keys script end -------------"
