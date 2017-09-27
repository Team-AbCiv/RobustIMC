/*
 * Copyright 2017 3TUSK, et al.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.tritusk.robustimc;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Mod(modid = "robustimc", name = "RobustIMC", version = "@VERSION@", useMetadata = true,
    acceptedMinecraftVersions = "[1.11,)", acceptableRemoteVersions = "*")
public final class RobustIMC {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        final boolean debug = Boolean.parseBoolean(System.getProperty("robustimc.debug", "false"));
        Logger log;
        if (debug) {
            log = event.getModLog();
        } else {
            log = null;
        }
        try (FileInputStream json = FileUtils.openInputStream(new File(event.getModConfigurationDirectory(), "robustimc.json"))) {
            NBTTagCompound tag = JsonToNBT.getTagFromJson(IOUtils.toString(json, StandardCharsets.UTF_8));
            tag.getKeySet().forEach(key -> {
                NBTTagCompound message = tag.getCompoundTag(key);
                if (message.getSize() == 0) {
                    if (debug) {
                        log.warn("Message '{}' is either empty or invalid! It will be ignored", key);
                    }
                    return;
                }
                final String receiverMod = message.getString("modid");
                final String messageKey = message.getString("key");
                if (receiverMod.isEmpty() || messageKey.isEmpty()) {
                    if (debug) {
                        log.warn("Message '{}' has either empty receiver or empty message key! It will be ignored.", key);
                    }
                    return;
                }
                String messageType = message.getString("type").toLowerCase(Locale.ENGLISH);
                switch (messageType) {
                    case ("string"): {
                        FMLInterModComms.sendMessage(receiverMod, messageKey, message.getString("message"));
                        break;
                    }
                    case ("nbt"): {
                        FMLInterModComms.sendMessage(receiverMod, messageKey, message.getCompoundTag("message"));
                        break;
                    }
                    case ("item"):
                    case ("stack"):
                    case ("itemstack"): {
                        FMLInterModComms.sendMessage(receiverMod, messageKey, new ItemStack(message.getCompoundTag("message")));
                        break;
                    }
                    case ("rs"):
                    case ("resourcelocation"): {
                        NBTTagCompound content = message.getCompoundTag("message");
                        FMLInterModComms.sendMessage(receiverMod, messageKey, new ResourceLocation(content.getString("domain"), content.getString("path")));
                        break;
                    }
                    default: {
                        if (debug) {
                            log.warn("Message '{}' has invalid message type '{}'! It will be ignored.", key, messageType);
                        }
                        break;
                    }
                }
            });
        } catch (Exception e) {
            if (debug) {
                log.error("RobustIMC encountered with error while resolving IMC message. It will stop being functional.", e);
            }
        }
    }
}
