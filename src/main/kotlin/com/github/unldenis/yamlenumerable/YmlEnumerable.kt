package com.github.unldenis.yamlenumerable

import org.yaml.snakeyaml.Yaml
import java.util.*
import java.util.regex.Pattern

fun parse(x: String): String {

    val yaml = Yaml()

    val cfg: Map<String, Map<String, String>> = yaml.load(x)
    val messages = cfg.getOrDefault("messages", emptyMap())

    val buf = StringJoiner("\n\n\n")

    countCustomClass = 0

    messages.forEach { buf.add(parseLine(it.key, it.value)) }


    return MSG_TEMPLACE.format(buf.toString())
}

var countCustomClass = 0
private fun parseLine(key: String, value: String): String {
    val keyFormatted = key.replace("-", "_").uppercase()

    val valueParameters = getParameters(value)

    if (valueParameters.isEmpty()) {
        return "     public static final Message $keyFormatted = new  Message(\"$key\");"
    }

    // parameters
    val methodParamsStr = StringJoiner(", ")
    valueParameters.forEach { methodParamsStr.add("String $it") }

    val methodReplaceStr = StringBuilder();
    valueParameters.forEach { methodReplaceStr.append(".replace(\"{$it}\", $it)").append('\n') }

    val newClass = "Message" + "%03d".format(countCustomClass++);

    return """ 
     public static final $newClass $keyFormatted = new $newClass("$key");
     public static final class $newClass extends Message {
    
        $newClass(String key) { super(key); }
    
        @Override
        @Deprecated
        public void send(CommandSender sender) { throw new IllegalStateException(key); }
        
        public void send(CommandSender sender, $methodParamsStr) {
          check();
          final String x = message$methodReplaceStr;
          sender.sendMessage(ChatColor.translateAlternateColorCodes('&', x));
        }
    
     }
    """.trimIndent()

}


// PARAMETERS
private val regex: Pattern = Pattern.compile("\\{(.*?)}")

private fun getParameters(input: String): LinkedHashSet<String> {
    val matchList = LinkedHashSet<String>()
    val regexMatcher = regex.matcher(input)

    while (regexMatcher.find()) {
        matchList.add(regexMatcher.group(1))
    }
    return matchList
}


const val MSG_TEMPLACE = """
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class Message {


%s

  // REMEMBER TO CALL THIS
  public static void load(FileConfiguration config) {
    Message[] messages = Arrays.stream(Message.class.getDeclaredFields())
        .filter(field -> Modifier.isStatic(field.getModifiers()) && Message.class.isAssignableFrom(
            field.getType()))
        .map(field -> {
          try {
            return (Message) field.get(null);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .toArray(Message[]::new);

    for (String key : config.getConfigurationSection("messages").getKeys(true)) {

      for (Message msg : messages) {
        if (msg.key.equals(key)) {
          msg.message = config.getString("messages." + key);
        }
      }
    }
  }

  protected final String key;
  protected String message;

  Message(String key) {
    this.key = key;
  }

  protected void check() { if (message == null)  throw new NullPointerException("Key " + key + " has not a message"); }

  public void send(CommandSender sender) {
    check();
    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
  }

}
"""
