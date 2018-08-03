package io.github.bananapuncher714.locksmith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

public class Locksmith extends JavaPlugin {
	public static final String[] KEYS = new String[] { "Locksmith", "key" };
	public static final String BLANK_KEY = ChatColor.WHITE + "Blank Key";
	public static final String KEY = ChatColor.WHITE + "Key";
	
	@Override
	public List< String > onTabComplete( CommandSender sender, Command command, String label, String[] args ) {
		List< String > aos = new ArrayList< String >();
		List< String > completions = new ArrayList< String >();
		if ( args.length == 1 ) {
			if ( sender.hasPermission( "locksmith.use" ) ) {
				aos.add( "unlock" );
			}
			if ( sender.hasPermission( "locksmith.admin" ) ) {
				aos.add( "getkey" );
				aos.add( "getlock" );
			}
		}
		
		StringUtil.copyPartialMatches( args[ args.length - 1 ], aos, completions );
		Collections.sort( completions );
		return completions;
	}

	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args ) {
		if ( !( sender instanceof Player ) ) {
			sender.sendMessage( "You must be a player to run this command!" );
			return false;
		}
		Player player = ( Player ) sender;
		if ( !player.hasPermission( "locksmith.use" ) ) {
			player.sendMessage( ChatColor.RED + "You do not have permission to run this command!" );
			return false;
		}
		if ( args.length == 0 ) {
			Block block = player.getTargetBlock( ( Set< Material > ) null, 4 );
			ItemStack item = player.getItemInHand();
			if ( block.getState() instanceof InventoryHolder ) {
				String blockLock = getLock( block );
				if ( blockLock == null || blockLock.isEmpty() ) {
					if ( isKey( item ) ) {
						if ( isBlankKey( item ) ) {
							String key = generateKey();
							String lock = KEY + legacyEncode( key );
							setLock( block, lock );
							item = fillKey( item );
							ItemMeta meta = item.getItemMeta();
							meta.setDisplayName( lock );
							item.setItemMeta( meta );
							player.setItemInHand( item );
							player.sendMessage( "You have created a new key and locked this block!" );
						} else {
							ItemMeta meta = item.getItemMeta();
							String lock = meta.getDisplayName();
							if ( lock == null ) {
								throw new NullPointerException( "Key name cannot be null!" );
							}
							setLock( block, lock );
							player.sendMessage( "You have locked this block with an existing key!" );
						}
					} else {
						player.sendMessage( ChatColor.RED + "You must be holding a key!" );
					}
				} else {
					player.sendMessage( ChatColor.RED + "This block is already locked!" );
				}
			} else {
				player.sendMessage( ChatColor.RED + "You can only lock a container!" );
			}
		} else if ( args.length == 1 ) {
			if ( args[ 0 ].equalsIgnoreCase( "getkey" ) ) {
				if ( player.hasPermission( "locksmith.admin" ) ) {
					ItemStack item = getBlankKey();
					player.getInventory().addItem( item );
				} else {
					player.sendMessage( ChatColor.RED + "You do not have permission to run this command!" );
				}
			} else if ( args[ 0 ].equalsIgnoreCase( "unlock" ) ) {
				Block block = player.getTargetBlock( ( Set< Material > ) null, 4 );
				ItemStack item = player.getItemInHand();
				String blockLock = getLock( block );
				if ( blockLock == null || blockLock.isEmpty() ) {
					player.sendMessage( ChatColor.RED + "You cannot unlock an unlocked container!" );
				} else {
					if ( isKey( item ) ) {
						if ( isBlankKey( item ) ) {
							player.sendMessage( ChatColor.RED + "You cannot use a blank key to unlock this container!" );
						} else {
							ItemMeta meta = item.getItemMeta();
							String lock = meta.getDisplayName();
							if ( lock == null ) {
								throw new NullPointerException( "Key name cannot be null!" );
							}
							if ( blockLock.equalsIgnoreCase( lock ) ) {
								setLock( block, "" );
								player.sendMessage( "You have unlocked this block!" );
							} else {
								player.sendMessage( ChatColor.RED + "You must be holding the right lock to unlock!" );
							}
						}
					} else {
						player.sendMessage( ChatColor.RED + "You must be holding a key!" );
					}
				}
			} else if ( args[ 0 ].equalsIgnoreCase( "getlock" ) ) {
				if ( player.hasPermission( "locksmith.admin" ) ) {
					Block block = player.getTargetBlock( ( Set< Material > ) null, 4 );
					String blockLock = getLock( block );
					if ( blockLock == null || blockLock.isEmpty() ) {
						player.sendMessage( ChatColor.RED + "This block is not locked!" );
					} else {
						ItemStack item = getBlankKey();
						item = fillKey( item );
						ItemMeta meta = item.getItemMeta();
						meta.setDisplayName( blockLock );
						item.setItemMeta( meta );
						player.getInventory().addItem( item );
						player.sendMessage( "You have generated a new key from this container!" );
					}
				} else {
					player.sendMessage( ChatColor.RED + "You do not have permission to run this command!" );
				}
			}
		}
		return false;
	}
	
	public static String getLock( Block block ) {
		Object val = NBTEditor.getBlockTag( block, "Lock" );
		return val != null ? ( String ) val : null;
	}
	
	public static void setLock( Block block, String lock ) {
		NBTEditor.setBlockTag( block, lock, "Lock" );
	}
	
	public static ItemStack getBlankKey() {
		ItemStack item = new ItemStack( Material.TRIPWIRE_HOOK );
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName( BLANK_KEY );
		item.setItemMeta( meta );
		
		return NBTEditor.setItemTag( item, ( byte ) 0, ( Object[] ) KEYS );
	}
	
	public static ItemStack fillKey( ItemStack item ) {
		if ( !isKey( item ) ) {
			return item;
		} else {
			return NBTEditor.setItemTag( item, ( byte ) 1, ( Object[] ) KEYS );
		}
	}
	
	public static boolean isKey( ItemStack item ) {
		return NBTEditor.getItemTag( item, ( Object[] ) KEYS ) != null;
	}
	
	public static boolean isBlankKey( ItemStack item ) {
		Object key = NBTEditor.getItemTag( item, ( Object[] ) KEYS );
		if ( key == null ) {
			return false;
		}
		byte val = ( byte ) key;
		return val == 0;
	}
	
	public static String generateKey() {
		return UUID.randomUUID().toString();
	}
	
	public static String legacyEncode( String value ) {
		StringBuilder builder = new StringBuilder();
		for ( char ch : value.toCharArray() ) {
			builder.append( "\u00a7" + ch );
		}
		return builder.toString();
	}
}
