use strict;
use JSON;
#use HTTP::Daemon::SSL;
use HTTP::Daemon;
use HTTP::Response;
use Data::Dumper;

use Irssi;
#use Irssi::TextUI;

########## SESSIONS ##########

my %sessions;

sub new_session {
	my $sessionid = "S" . int(rand(1000000000000000));
	my $session = {id => $sessionid, events => []};
	$sessions{$sessionid} = $session;
	add_init_events($session);
	#debug("creating new session:" . Dumper($session));
	return $sessions{$sessionid};
}

########## HTTP ##########

my $daemon = HTTP::Daemon->new(
	LocalPort => 38444,
	Reuse => 1,
	Blocking => 0
) || die;
Irssi::print('Webssi listening at: ' . $daemon->url);

Irssi::input_add(fileno($daemon), INPUT_READ, \&handle_connection,0);

my %tag;

sub handle_connection {
	my $client = $daemon->accept;
	$tag{$client} = Irssi::input_add(fileno($client), INPUT_READ, \&handle_request, $client);
}

sub handle_request($) {
	my ($client) = @_;
	#debug('Getting request from ' . $client);	
	my $request = $client->get_request;
	if (!$request) {
		#debug("closing connection to $client");
		# TODO if it's a pending_client of a connection, abort that one
		Irssi::input_remove($tag{$client});
		$client->close;
		delete $tag{$client};
		return;
	}

	my $url = $request->url->path;
	#debug('Request: method:' . $request->method . " url:\"$url\"");
	
	if (! check_request_authorization($request)) {
		send_authorization_required($client);
	} elsif ($url eq '/events.json') {
		my ($sessionid, $commands) = split / /, $request->content(), 2;
		
		# session
		my $session;
		if ($sessionid eq 'newSession') {
			$session = new_session();
		} else {
			$session = $sessions{$sessionid};
			if (!$session) {
				my $response = HTTP::Response->new(200, undef, undef, "[{\"type\":\"unknown session\"}]");
				print_warn("unknown session $sessionid");
				$client->send_response($response);
				return;
			}
		}
		
		if ($session->{pending_client}) {
			debug("dropping older request");
			my $client = $session->{pending_client};
			my $response = HTTP::Response->new(200, undef, undef, "[{\"type\":\"request superseded\"}]");
			$client->send_response($response);
			delete $session->{pending_client};
		}
		
		$session->{pending_client} = $client;
		
		# commands
		processCommands($session, $commands);
		
		send_response_later($session);
	} elsif ($url =~ /^(\/[a-zA-Z0-9_]+)*\/[a-zA-Z0-9_\.]*$/ ) {
		if ($url eq '/') {
			$url = '/Webssi.html';
		}
		my $filename = Irssi::get_irssi_dir() . '/webssi' . $url;
		debug("Returning file $filename");
		$client->send_file_response($filename);
	} else {
		debug('403 bad chars');
		my $response = HTTP::Response->new(403, undef, undef, 'URL contains characters that are not allowed.');
		$client->send_response($response);
	}
}

sub send_response_later($) {
	my ($session) = @_;
	if ($session->{response_scheduled} || ! $session->{pending_client} || scalar(@{$session->{events}}) == 0) {
		return;
	}
	debug("send_response_later: " . scalar(@{$session->{events}}) . " events waiting");
	$session->{response_scheduled} = 1;
	Irssi::timeout_add_once(50, \&send_response_now, $session);
}

sub send_response_now($) {
	my ($session) = @_;
	
	my $client = $session->{pending_client};
	
	if(!$client) {
		die "no connection to send response to";
	}
	
	before_send_response();
	
	# get events
	my $eventstring = pop_events_as_json($session);
	
	# send reply
	my $response = HTTP::Response->new(200, undef, undef, $eventstring);
	#debug("sending: $eventstring");
	$client->send_response($response);
	
	delete $session->{pending_client};
	$session->{response_scheduled} = 0;
}

sub check_request_authorization($) {
	my ($request) = @_;
	if (Irssi::settings_get_bool('webssi_authentication_disabled')) {
		return 1;
	}
	my ($request_username, $request_password) = $request->authorization_basic;
	if (! defined($request_username)) {
		debug("no authorization");
		return 0;
	}
	my $config_password = Irssi::settings_get_str('webssi_password');
	if ($request_password eq $config_password) {
		debug("Password ok");
		return 1;
	} else {
		debug("Password not ok");
		return 0;
	}
}

sub send_authorization_required($) {
	my ($client) = @_;
	
	my $response = HTTP::Response->new(401, "Unauthorized", undef, "Unauthorized\n");
	$response->header('WWW-Authenticate' => 'Basic realm="Webssi"');
	$client->send_response($response);
}

Irssi::settings_add_str('webssi', 'webssi_password', '');
Irssi::settings_add_bool('webssi', 'webssi_authentication_disabled', 0);

########## EVENTS ##########

sub add_event($$) {
	my ($session, $event) = @_;
	push @{$session->{events}}, $event;
	send_response_later($session);
}

# adds event to all logged in sessions
sub add_event_all($) {
	my ($event) = @_;
	for my $session (values(%sessions)) {
		add_event($session, $event);
	}
}

# return the pending events for the given session encoded as json, and clear the list
sub pop_events_as_json($) {
	my ($session) = @_;
	my $jsonstring = encode_json($session->{events});
	$session->{events} = [];
	return $jsonstring;
}

########## CREATE EVENTS ##########

sub ev($$) {
	my ($type, $args) = @_;
	$args->{type} = $type;
	return $args;
}

sub ev_window($$$) {
	my ($type, $win, $args) = @_;
	my $result = ev($type, $args);
	$result->{window} = window_to_id($win);
	return $result;
}

# window is passed here as an extra argument because $item->window() isn't always what we want it to be
# when item is being moved, removed, or is null
sub ev_window_item($$$$) {
	my ($type, $window, $item, $args) = @_;
	my $result = ev_window($type, $window, $args);
	$result->{item} = item_to_id($item);
	if ($item->{server}) {
		$result->{tag} = $item->{server}->{tag};
	}
	return $result;
}

sub ev_channel($$$) {
	my ($type, $channel, $args) = @_;
	return ev_window_item($type, $channel->window(), $channel, $args);
}

sub ev_text($$) {
	my ($win, $text) = @_;
	return ev_window('T', $win, {text => $text});
}

sub ev_nicklist($$$$) {
	my ($type, $channel, $nick, $args) = @_;
	my $result = ev_channel($type, $channel, $args);
	$result->{name} = $nick->{nick};
	return $result;
}

sub ev_nicklist_new($$) {
	my ($channel, $nick) = @_;
	return ev_nicklist('nicklist new', $channel, $nick, {});
}

sub ev_window_item_new($$) {
	my ($win, $item) = @_;
	my $result = ev_window_item('window item new', $win, $item, {
		visible_name => $item->{visible_name},
		is_active => $item->is_active()
	});
	if ($item->isa('Irssi::Channel')) {
		$result->{'item_type'} = 'channel';
	}
	return $result;
}

sub ev_server($$$) {
	my ($type, $server, $args) = @_;
	my $result = ev($type, $args);
	$result->{tag} = $server->{tag};
	return $result;
}

sub ev_server_new($) {
	my ($server) = @_;
	return ev_server('server new', $server, {});
}

########## OBJECT MAPPING ##########

sub map_object($) {
	my ($object) = @_;
	if (! defined($object) || $object == 0) { # TODO no way to see difference between NULL object and number 0? => need type info...
		return undef;
	}
	
	my $type = ref $object;
	if ($type eq 'Irssi::UI::Window') {
		return window_to_id($object);
	} else {
		die "unknown type $type of object '$object'";
	}
}

sub map_full_object($) {
	my ($object) = @_;
	my $type = ref $object;
	if ($type eq 'Irssi::UI::Window') {
		return map_full_window($object);
	} else {
		die "unknown type $type of object '$object'";
	}
}

sub window_to_id($) {
	my ($window) = @_;
	#return "" . $window->{'refnum'};
	return '' . $window->{'_irssi'};
}

sub id_to_window($) {
	my ($id) = @_;
	return (grep {$_->{'_irssi'} eq $id} Irssi::windows())[0];
}

sub map_full_window($) {
	my ($window) = @_;
	
	return {
		window => window_to_id($window),
		name => $window->{'name'},
		refnum => $window->{'refnum'}
	};
}

sub item_to_id($) {
	my ($item) = @_;
	if (! defined($item)) {
		return undef;
	}
	my $tag = $item->{server} ? $item->{server}->{tag} . ' ': 'itemWithoutServer';
	return ($tag . $item->{name});
}

sub id_to_item($) {
	my ($id) = @_;
	my ($tag, $name) = split(/ /, $id);
	if (defined($name)) {
		my $server = Irssi::server_find_tag($tag);
		return $server ? $server->window_item_find($name) : undef;
	} else {
		return Irssi::window_item_find($id);
	}
}

sub map_full_nick($) {
	my ($nick) = @_;
	return {
		#'id' => nick_to_id($nick),
		'name' => $nick->{nick}
	}
}

########## INIT EVENTS ##########

sub add_init_events($) {
	my ($session) = @_;
	add_event($session, ev('init', {sessionid => $session->{id}}));
	foreach my $server (Irssi::servers()) {
		add_event($session, ev_server_new($server));
	}
	foreach my $win (Irssi::windows()) {
		add_event($session, ev('window new', map_full_window($win)));
		foreach my $item ($win->items()) {
			add_event($session, ev_window_item_new($win, $item));
			if ($item->isa('Irssi::Channel')) {
				foreach my $nick ($item->nicks()) {
					add_event($session, ev_nicklist_new($item, $nick));
				}
			}
		}
	}
	add_event($session, ev('window changed', {'window' => window_to_id(Irssi::active_win())}));
}

########## SIGNALS ##########

# $param_names = array of names of parameters, or undefined if there is only one, unnamed param
# $param_full = index of parameter that has to be mapped fully, or -1 if none
sub register_standard_signal {
	my ($signal_name, $param_names, $param_full) = @_;
	
	if (!defined($param_full)) {
		$param_full = -1
	}
	Irssi::signal_add($signal_name, sub {
		my $params = {};
		my $param_index = 0;
		if (defined($param_names)) {
			foreach my $param (@_) {
				if ($param_index == $param_full) {
					$params->{$param_names->[$param_index++]} = map_full_object($param);
				} else {
					$params->{$param_names->[$param_index++]} = map_object($param);
				}
			}
		} else {
			# assert param_full == 0 && scalar(@_) == 1
			$params = map_full_object($_[0]);
		}
		add_event_all(ev($signal_name, $params));
	});
}

Irssi::signal_add('window created', sub {
	my ($win) = @_;
	add_event_all(ev('window new', map_full_window($win)));
});

Irssi::signal_add('window destroyed', sub {
	my ($win) = @_;
	add_event_all(ev('window remove', {window => window_to_id($win)}));
});

register_standard_signal('window changed', ['window', 'old']);
#register_standard_signal('window changed automatic', ['window', 'old']);

Irssi::signal_add('window item new', sub {
	my ($win, $item) = @_;
	add_event_all(ev_window_item_new($win,$item));
});

Irssi::signal_add('window item remove', sub {
	my ($window, $item) = @_;
	add_event_all(ev_window_item('window item remove', $window, $item, {}));
});

Irssi::signal_add('window item moved', sub {
	my ($window, $item, $old_window) = @_;
	add_event_all(ev_window_item('window item moved', $old_window, $item, {new_window_event => ev_window(undef, $window, {})}));
});

Irssi::signal_add('window item changed', sub {
	my ($window, $item) = @_;
	# note: $item might be null here
	add_event_all(ev_window('window item changed', $window, {item => item_to_id($item)}));
});

Irssi::signal_add('nicklist new', sub {
	my ($channel, $nick) = @_;
	add_event_all(ev_nicklist_new($channel, $nick));
});

Irssi::signal_add('nicklist remove', sub {
	my ($channel, $nick) = @_;
	if ($channel->window()) { # ignore events for a channel that's just been removed
		add_event_all(ev_nicklist('nicklist remove', $channel, $nick, {}));
	}
});

Irssi::signal_add('nicklist changed', sub {
	my ($channel, $nick, $old_name) = @_;
	# can't use ev_nicklist here because the nick $nick has already changed.
	# we send the event with the old nick, passing the new one as extra arg
	add_event_all(ev_channel('nicklist changed', $channel, {
		name => $old_name,
		new_name => $nick->{nick}
	}));
});

Irssi::signal_add('window refnum changed', sub {
	my ($window, $old) = @_;
	add_event_all(ev('window refnum changed', {'window' => window_to_id($window), 'refnum' => $window->{'refnum'}}));
});

Irssi::signal_add('window name changed', sub {
	my ($window, $old) = @_;
	add_event_all(ev('window name changed', {'window' => window_to_id($window), 'name' => $window->{'name'}}));
});

Irssi::signal_add('server connected', sub {
	my ($server) = @_;
	add_event_all(ev('server new', {tag => $server->{tag}}));
});

Irssi::signal_add('server disconnected', sub {
	my ($server) = @_;
	add_event_all(ev('server remove', {tag => $server->{tag}}));
});

# Catch 'print text' as late as possible (Irssi::SIGNAL_PRIORITY_LOW == 100)
Irssi::signal_add_priority('print text', \&sig_print_text, 200);
# set to a positive number if lines being printed should be ignored (to prevent infinite recursion on errors or debug messages) 
my $ignore_printing = 0;

sub sig_print_text {
	my ($dest, $text, $stripped) = @_;
	
	if ($ignore_printing || $stripped =~ /DEBUG/ || $stripped =~ /^\t/) { # TODO remove this exceptions...
		return;
	}
	$ignore_printing++;
	
	update_text($dest->{window});
#
#	add_event_all(ev_text($dest->{'window'}, $stripped));
#	#add_event_all(ev_text($dest->{'window'}, text_to_html($text)));
#	
	$ignore_printing--;
}

# add events for new text in the given window to the clients
sub update_text {
	my ($window) = @_;

	my $view = $window->view;
	
	my $line;
	my $last_sent_line = $view->get_bookmark('webssi');
	if ($last_sent_line) {
		$line = $last_sent_line->next;
	} else {
		$line = $view->get_lines;
	}
	
	while ($line) {
		my $text = $line->get_text(1);
		if ($text !~ /DEBUG/) {
			add_event_all(ev_text($window, text_to_html($text)));
		}
		$line = $line->next;
	}
	
	$view->set_bookmark_bottom('webssi')
}

#Irssi::signal_add('gui print text', \&sig_gui_print_text);
#sub sig_gui_print_text {
#	my ($win, $fg, $bg, $flags, $text, $dest) = @_;
#	if ($ignore_printing || $text =~ /DEBUG/) {
#		return;
#	}
#	$ignore_printing++;
#	add_event_all({ type => 'T', text => $text});
#	$ignore_printing--;
#}

my $last_entry;
my $last_entry_pos;

sub update_entry {
	my $entry = Irssi::parse_special('$L');
	my $entry_pos = Irssi::gui_input_get_pos();
	return if (defined($last_entry) && $entry eq $last_entry && $last_entry_pos == $entry_pos);
	add_event_all(ev('entry changed', {content => $entry, cursorPos => $entry_pos }));
	$last_entry = $entry;
	$last_entry_pos = $entry_pos;
	#debug("entry: " . $entry);
}

Irssi::signal_add_last('gui key pressed', sub {
	update_entry();
	#debug("key:" . $_[0])
});

sub before_send_response() {
	update_entry();
}

########## COMMANDS ##########

sub processCommands($$) {
	my ($session, $commandstring) = @_;
	return unless defined($commandstring);
	my $commands = decode_json($commandstring);
	if (ref $commands ne 'ARRAY') {
		debug("ERROR: $commands isn't an array");
		return;
	}
	
	return unless (scalar(@$commands)); # return if there are no events
	
	foreach my $command (@$commands) {
		if (ref $command ne 'HASH') {
			debug("ERROR: $command isn't a hash");
			return;
		}
		
		# signal that we start processing the command
		add_event($session, ev('command', {'id' => $command->{'id'}}));

		if ($command->{'type'} eq 'sendLine') {
			my $win = id_to_window($command->{'win'});
			my $line = $command->{'line'};
			cmd_send_line($win, $line);
		} elsif ($command->{type} eq 'activateWindow') {
			my $win = id_to_window($command->{'win'});
			cmd_activate_window($win);
		} elsif ($command->{type} eq 'activateWindowItem') {
			my $item = id_to_item($command->{'item'});
			cmd_activate_window_item($item);
		} elsif ($command->{type} eq 'key') {
			my $keys = $command->{'keys'};
			cmd_key($keys);
		}
	}
	
	# not processing any command anymore
	add_event($session, ev('command', {'id' => undef}));
}

sub cmd_send_line($$) {
	my ($win, $line) = @_;
	my $command = ($line =~ /^\//) ? $line : "MSG * $line";
	if (! $win) {
		$win = Irssi::active_win();
	}
	
	$win->command($command);
}

sub cmd_activate_window($) {
	my ($win) = @_;
	$win->set_active();
}

sub cmd_activate_window_item($) {
	my ($item) = @_;
	$item->set_active();
}

sub cmd_key($) {
	my ($keys) = @_;
	foreach my $key (@$keys) {
		Irssi::signal_emit('gui key pressed', $key)
	}
}

########## DEBUG ##########

sub debug {
	Irssi::print("DEBUG: " . $_[0]);
}

sub print_warn {
	Irssi::print("WARN: " . $_[0]);
}


########## COLORS ##########
# based on log2ansi.pl by Peder Stray

my (%attr, %old);

#my @bols = qw(bold underline blink reverse fgh bgh);
#my @nums = qw(fgc bgc);

my $n = 0;
my %ansi = map { $_ => $n++ } split //, 'krgybmcw';
my @irssi = split //, 'kbgcrmywKBGCRMYW';

my @webssi_fg = qw(fk fb fg fc fr fm fy fw lK lB lG lC lR lM lY lW);
my @webssi_bg = qw(bk bb bg bc br bm by bw oK oB oG oC oR oM oY oW);

my @ic = map {$ansi{lc $_}} @irssi;
my @ih = map {$_ eq uc $_} @irssi;

sub defc {
    my($attr) = shift || \%attr;
    #$attr->{fgc} = $attr->{bgc} = -1;
    #$attr->{fgh} = $attr->{bgh} = 0;
    $attr->{fg} = $attr->{bg} = -1;
}

sub defm {
    my($attr) = shift || \%attr;
    $attr->{bold} = $attr->{underline} = 
      $attr->{blink} = $attr->{reverse} = $attr->{monospace} = 0;
}

sub def {
    my($attr) = shift || \%attr;
    defc($attr);
    defm($attr);
}

sub setold {
    %old = %attr;
}

my $buffer;

my %html_escaped = (
	'<' => '&lt;',
	'>' => '&gt;',
	'&' => '&amp;'
);

sub emit($) {
	my ($text) = @_;
	
	# escape special chars
	$text =~ s/([<>&])/$html_escaped{$1}/ge;
	
	my $class = '';
	$class .= 'b ' if $attr{bold};
	$class .= 'u ' if $attr{underline};
	$class .= 'm ' if $attr{monospace};
	#$class .= 'l ' if $attr->{blink};
	#$class .= 'r ' if $attr->{reverse};
	my $fg = $attr{fg};
	my $bg = $attr{bg};
	if ($attr{reverse}) {
		my $tmp = $bg;
		$bg = $fg;
		$fg = $tmp;
	}
	if (defined($fg) && $fg != -1) {
		$class .= $webssi_fg[$fg] . ' ';
	}
	if (defined($bg) && $bg != -1) {
		$class .= $webssi_bg[$bg] . ' ';
	}
	
	#$buffer .= '</span>';
	if ($class ne '') {
		#debug("class='$class' fg='$fg' bg='$bg' text='$text'");
		chop $class; # chop space
		$buffer .= "<span class='$class'>";
		$buffer .= $text;
		$buffer .= '</span>';
	} else {
		$buffer .= $text;
	}
}

sub text_to_html($) {
    ($_) = @_;
    def;
    setold;
    $buffer = '';
    my $buffer_has_indent = 0;
    my $prefix = '<div>';
    my $suffix = '</div>';

    while (length) {
	if (s/^\cD//) {
	    if (s/^a//) {
		$attr{blink} = !$attr{blink};
	    } elsif (s/^b//) {
		$attr{underline} = !$attr{underline};
	    } elsif (s/^c//) {
		$attr{bold} = !$attr{bold};
	    } elsif (s/^d//) {
		$attr{reverse} = !$attr{reverse};
	    } elsif (s/^e//) {
		if (!$buffer_has_indent) {
			$buffer_has_indent = 1;
			$prefix .= "<div class='p3'><div class='p1'>";
			$buffer .= "</div><div class='p2'>";
			$suffix = "</div></div>" . $suffix;
		}
	    } elsif (s/^f([^,]*),//) {
		# indent_func
	    } elsif (s/^g//) {
		def;
	    } elsif (s/^h//) {
		# clrtoeol
	    } elsif (s/^i//) {
		$attr{monospace} = ! $attr{monospace};
		debug('monospace');
	    #} elsif (s/^([\/-?])([\/-?])//) {
	    } elsif (s/^(.)(.)//) {
		$attr{fg} = (ord($1) == 255 || $1 eq '/') ? -1 : ((ord($1)-ord('0')) % 16);
		$attr{bg} = (ord($2) == 255 || $2 eq '/') ? -1 : ((ord($2)-ord('0')) % 16);
	    }
	} elsif (s/^\c_//) {
	    $attr{underline} = !$attr{underline};
	} elsif (s/^\cV//) {
	    $attr{reverse} = !$attr{reverse};
	} else {
	    #s/^(.[^\cB\cC\cD\cF\cO\cV\c[\c_]*)//;
	    s/^(.[^\cD\c_\cV]*)//;
	    emit $1;
	}
    }

    return $prefix . $buffer . $suffix;
}
