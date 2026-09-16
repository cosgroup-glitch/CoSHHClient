package me.ender;

import haven.*;
import haven.sprites.PingSprite;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatCommands {
    public static void sendGobHighlight(UI ui, long gobId) {
	if(!CFG.MAZES_SEND_PINGS.get()) {return;}
	ChatUI.EntryChannel channel = findChannelForCommand(ui);
	if(channel != null) {
	    channel.send(String.format("@%d", gobId));
	}
    }

    public static void sendPartyGobHighlight(UI ui, long gobId) {
	if(!CFG.MAZES_SEND_PINGS.get()) {return;}
	ChatUI.EntryChannel channel = findPartyChannel(ui);
	if(channel != null) {
	    channel.send(String.format("@%d", gobId));
	}
    }

    public static void sendPartyPriorityTarget(UI ui, long gobId) {
	if(!CFG.MAZES_SEND_PINGS.get()) {return;}
	ChatUI.EntryChannel channel = findPartyChannel(ui);
	if(channel != null) {
	    channel.send(String.format("@!%d", gobId));
	}
    }

    public static void sendPartyTargetMarker(UI ui, Gob gob) {
	if(!CFG.MAZES_SEND_PINGS.get()) {return;}
	ChatUI.EntryChannel channel = findPartyChannel(ui);
	if(channel != null) {
	    channel.send(ui.sess.glob.party.markNext(gob));
	}
    }
    
    public static void sendPointHighlight(UI ui, long gridId, Coord offset) {
	if(!CFG.MAZES_SEND_PINGS.get()) {return;}
	ChatUI.EntryChannel channel = findChannelForCommand(ui);
	if(channel != null) {
	    channel.send(String.format("@%d;%d;%d", gridId, offset.x, offset.y));
	}
    }

    public static void sendPartyMapPing(UI ui, Coord offset) {
	if(!CFG.MAZES_SEND_PINGS.get()) {return;}
	ChatUI.EntryChannel channel = findPartyChannel(ui);
	if(channel != null) {
	    channel.send(String.format("LOC@%dx%d", offset.x, offset.y));
	}
    }
    
    private static ChatUI.EntryChannel findPartyChannel(UI ui) {
	ChatUI chat = ui.gui.chat;
	for (ChatUI.Selector.DarkChannel chl : chat.chansel.chls) {
	    if(chl.chan instanceof ChatUI.PartyChat) {
		return (ChatUI.EntryChannel) chl.chan;
	    }
	}
	return null;
    }

    private static ChatUI.EntryChannel findChannelForCommand(UI ui) {
	ChatUI chat = ui.gui.chat;
	ChatUI.Channel selected = chat.sel;
	
	if(selected instanceof ChatUI.PrivChat) {
	    return (ChatUI.EntryChannel) selected;
	}
	
	ChatUI.EntryChannel area = null;
	ChatUI.EntryChannel party = null;
	
	for (ChatUI.Selector.DarkChannel chl : chat.chansel.chls) {
	    if(chl.chan instanceof ChatUI.PartyChat) {
		party = (ChatUI.EntryChannel) chl.chan;
	    } else if(chl.chan instanceof ChatUI.MultiChat && "Area Chat".equals(chl.rname.text)) {
		area = (ChatUI.EntryChannel) chl.chan;
	    }
	}
	
	return party != null ? party : area;
    }
    
    private static final Pattern HIGHLIGHT = Pattern.compile("^@(!?)(-?\\d+;-?\\d+;)?(-?\\d+)$");
    private static final Pattern LEADER_PING = Pattern.compile("^LPING@(-?\\d+)$");
    private static final Pattern LOCATION_PING = Pattern.compile("^LOC@(-?\\d+)x(-?\\d+)$");
    
    public static boolean matchesCommand(String msg) {
	return HIGHLIGHT.matcher(msg).matches() || LEADER_PING.matcher(msg).matches() ||
	    LOCATION_PING.matcher(msg).matches() || Party.isTargetMarkerMessage(msg);
    }
    
    public static boolean processCommand(UI ui, String msg) {
	return processCommand(ui, msg, -1);
    }

    public static boolean processCommand(UI ui, String msg, long fromGobid) {
	if(Party.isTargetMarkerMessage(msg)) {
	    ui.sess.glob.party.handleMarkerMessage(msg);
	    return true;
	}
	Matcher lp = LEADER_PING.matcher(msg);
	if(lp.matches()) {
	    try {
		Gob gob = ui.gui.map.glob.oc.getgob(Long.parseLong(lp.group(1)));
		if(gob != null) {
		    gob.highlight();
		    ui.root.effects.markGob(gob, 7);
		}
	    } catch (Exception ignored) {}
	    return true;
	}
	Matcher loc = LOCATION_PING.matcher(msg);
	if(loc.matches()) {
	    handleLocationPing(ui, loc, fromGobid);
	    return true;
	}
	Matcher matcher = HIGHLIGHT.matcher(msg);
	if(matcher.matches()) {
	    try {
		boolean priority = "!".equals(matcher.group(1));
		String encoded = (matcher.group(2) == null ? "" : matcher.group(2)) + matcher.group(3);
		String[] parts = encoded.split(";");
		if(parts.length == 1) {
		    Gob gob = ui.gui.map.glob.oc.getgob(Long.parseLong(parts[0]));
		    if(gob != null) {
			gob.highlight();
			ui.root.effects.markGob(gob, priority ? 30 : 7);
			return true;
		    }
		} else if(parts.length == 3) {
		    Coord offset = Coord.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
		    MCache.Grid grid = ui.sess.glob.map.getgrid(Long.parseLong(parts[0]));
		    if(grid != null) {
			ui.root.effects.markPoint(grid, offset);
			return true;
		    }
		}
	    } catch (Exception ignored) {}
	}
	return false;
    }

    private static void handleLocationPing(UI ui, Matcher matcher, long fromGobid) {
	try {
	    if(fromGobid < 0 || ui.gui == null || ui.gui.map == null || ui.gui.mapfile == null)
		return;
	    Party.Member pm;
	    synchronized (ui.sess.glob.party.memb) {
		pm = ui.sess.glob.party.memb.get(fromGobid);
	    }
	    Gob player = ui.gui.map.player();
	    if(player == null || pm == null)
		return;
	    Coord2d playerc = player.rc;
	    Coord2d partyc = pm.getc();
	    if(partyc == null || playerc.dist(partyc) >= 975 * 11)
		return;
	    Coord2d offset = Coord2d.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
	    ui.gui.mapfile.view.addSprite(new PingSprite(partyc.add(offset), pm.col, 4));
	} catch (Exception ignored) {}
    }
}
