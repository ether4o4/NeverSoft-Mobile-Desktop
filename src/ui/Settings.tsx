import React from 'react';
import {
  Modal,
  View,
  Text,
  TextInput,
  Pressable,
  ScrollView,
  StyleSheet,
} from 'react-native';
import {theme} from '../theme';
import {useStore, actions} from '../store';
import {CLOUD_PRESETS, LOCAL_MODELS} from '../models';
import {localAvailable} from '../llm/localLlama';

export default function Settings() {
  const open = useStore(s => s.settingsOpen);
  const settings = useStore(s => s.settings);
  const close = () => actions.openSettings(false);
  const cloud = settings.provider === 'cloud';
  const engineReady = localAvailable();

  return (
    <Modal visible={open} transparent animationType="slide" onRequestClose={close}>
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <View style={styles.header}>
            <Text style={styles.title}>
              <Text style={styles.titleMark}>MVE</Text> · settings
            </Text>
            <Pressable onPress={close} hitSlop={10}>
              <Text style={styles.close}>Done</Text>
            </Pressable>
          </View>

          {/* provider toggle */}
          <View style={styles.segment}>
            <Seg label="Cloud" active={cloud} onPress={() => actions.updateSettings({provider: 'cloud'})} />
            <Seg
              label="On-device"
              active={!cloud}
              onPress={() => actions.updateSettings({provider: 'local'})}
            />
          </View>

          <ScrollView style={styles.body} keyboardShouldPersistTaps="handled">
            {cloud ? (
              <>
                <Text style={styles.section}>Provider preset</Text>
                <View style={styles.presetRow}>
                  {CLOUD_PRESETS.map(p => (
                    <Pressable
                      key={p.id}
                      style={[
                        styles.preset,
                        settings.cloudBaseUrl === p.baseUrl && styles.presetOn,
                      ]}
                      onPress={() =>
                        actions.updateSettings({cloudBaseUrl: p.baseUrl, cloudModel: p.model})
                      }>
                      <Text style={styles.presetText}>{p.name}</Text>
                    </Pressable>
                  ))}
                </View>

                <Field
                  label="Base URL"
                  value={settings.cloudBaseUrl}
                  onChangeText={t => actions.updateSettings({cloudBaseUrl: t})}
                  placeholder="https://api.openai.com/v1"
                  autoCapitalize="none"
                />
                <Field
                  label="Model"
                  value={settings.cloudModel}
                  onChangeText={t => actions.updateSettings({cloudModel: t})}
                  placeholder="gpt-4o-mini"
                  autoCapitalize="none"
                />
                <Field
                  label="API key"
                  value={settings.cloudKey}
                  onChangeText={t => actions.updateSettings({cloudKey: t})}
                  placeholder="sk-…"
                  autoCapitalize="none"
                  secureTextEntry
                />
                <Text style={styles.hint}>
                  Your key is stored only on this device and sent straight to the endpoint you chose.
                </Text>
              </>
            ) : (
              <>
                <View style={[styles.status, engineReady ? styles.statusOk : styles.statusWarn]}>
                  <Text style={styles.statusText}>
                    {engineReady
                      ? 'On-device engine ready — pick a model to download and run locally.'
                      : 'On-device engine isn’t in this build yet. Selection is saved; local inference lands in the engine build.'}
                  </Text>
                </View>
                <Text style={styles.section}>Local model</Text>
                {LOCAL_MODELS.map(m => {
                  const on = settings.localModelId === m.id;
                  return (
                    <Pressable
                      key={m.id}
                      style={[styles.model, on && styles.modelOn]}
                      onPress={() => actions.updateSettings({localModelId: m.id})}>
                      <View style={[styles.radio, on && styles.radioOn]} />
                      <View style={styles.modelInfo}>
                        <Text style={styles.modelName}>
                          {m.name} <Text style={styles.modelMeta}>· {m.params} · {m.size}</Text>
                        </Text>
                        <Text style={styles.modelNote}>{m.note}</Text>
                      </View>
                    </Pressable>
                  );
                })}
                <Text style={styles.hint}>Models are GGUF files pulled from Hugging Face and run fully on-device.</Text>
              </>
            )}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

function Seg({label, active, onPress}: {label: string; active: boolean; onPress: () => void}) {
  return (
    <Pressable style={[styles.seg, active && styles.segOn]} onPress={onPress}>
      <Text style={[styles.segText, active && styles.segTextOn]}>{label}</Text>
    </Pressable>
  );
}

function Field(props: {
  label: string;
  value: string;
  onChangeText: (t: string) => void;
  placeholder?: string;
  autoCapitalize?: 'none' | 'sentences';
  secureTextEntry?: boolean;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{props.label}</Text>
      <TextInput
        style={styles.fieldInput}
        value={props.value}
        onChangeText={props.onChangeText}
        placeholder={props.placeholder}
        placeholderTextColor={theme.textFaint}
        autoCapitalize={props.autoCapitalize}
        secureTextEntry={props.secureTextEntry}
        autoCorrect={false}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  backdrop: {flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'flex-end'},
  sheet: {
    backgroundColor: theme.surface,
    borderTopLeftRadius: 14,
    borderTopRightRadius: 14,
    borderWidth: 1,
    borderColor: theme.border,
    maxHeight: '86%',
    paddingBottom: 20,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
  },
  title: {fontSize: 15, color: theme.text},
  titleMark: {fontFamily: theme.mono, color: theme.red, fontWeight: '800', letterSpacing: 1},
  close: {color: theme.purple, fontSize: 14, fontWeight: '600'},
  segment: {
    flexDirection: 'row',
    marginHorizontal: 16,
    borderWidth: 1,
    borderColor: theme.border,
    borderRadius: 4,
    overflow: 'hidden',
  },
  seg: {flex: 1, paddingVertical: 9, alignItems: 'center', backgroundColor: theme.bg},
  segOn: {backgroundColor: theme.purpleSoft},
  segText: {fontFamily: theme.mono, fontSize: 12, color: theme.textDim},
  segTextOn: {color: theme.text},
  body: {paddingHorizontal: 16, paddingTop: 14},
  section: {
    fontFamily: theme.mono,
    fontSize: 10,
    letterSpacing: 1.5,
    textTransform: 'uppercase',
    color: theme.textFaint,
    marginBottom: 8,
    marginTop: 6,
  },
  presetRow: {flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 14},
  preset: {
    borderWidth: 1,
    borderColor: theme.border,
    borderRadius: 4,
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  presetOn: {borderColor: theme.purple, backgroundColor: theme.purpleSoft},
  presetText: {color: theme.text, fontSize: 13},
  field: {marginBottom: 12},
  fieldLabel: {fontSize: 12, color: theme.textDim, marginBottom: 5},
  fieldInput: {
    backgroundColor: theme.bg,
    borderWidth: 1,
    borderColor: theme.border,
    borderRadius: 4,
    paddingHorizontal: 12,
    paddingVertical: 9,
    color: theme.text,
    fontSize: 14,
  },
  hint: {color: theme.textFaint, fontSize: 12, lineHeight: 18, marginTop: 4, marginBottom: 12},
  status: {borderWidth: 1, borderRadius: 4, padding: 11, marginBottom: 16},
  statusOk: {borderColor: theme.purple, backgroundColor: theme.purpleSoft},
  statusWarn: {borderColor: theme.red, backgroundColor: theme.redSoft},
  statusText: {color: theme.text, fontSize: 12.5, lineHeight: 18},
  model: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderWidth: 1,
    borderColor: theme.border,
    borderRadius: 4,
    padding: 12,
    marginBottom: 8,
  },
  modelOn: {borderColor: theme.purple, backgroundColor: theme.purpleSoft},
  radio: {
    width: 16,
    height: 16,
    borderRadius: 8,
    borderWidth: 2,
    borderColor: theme.textFaint,
  },
  radioOn: {borderColor: theme.purple, backgroundColor: theme.purple},
  modelInfo: {flex: 1},
  modelName: {color: theme.text, fontSize: 14},
  modelMeta: {color: theme.textFaint, fontSize: 12},
  modelNote: {color: theme.textDim, fontSize: 12, marginTop: 2},
});
