# SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
#
# SPDX-License-Identifier: GPL-2.0-or-later





print "--------------- tn5250j test fields script start ------------"

screen = _session.getScreen()

screenfields = screen.getScreenFields()

fields = screenfields.getFields()

for x in fields:
    print x.toString()
    print x.getString()

print "number of fields %s " % screenfields.getSize()

print "---------------- tn5250j test fields script end -------------"
