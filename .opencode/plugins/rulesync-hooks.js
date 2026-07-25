export const RulesyncHooksPlugin = async ({ $ }) => {
  return {
    "tool.execute.after": async (input) => {
      const __re = new RegExp("Write|Edit");
      if (__re.test(input.tool)) {
        await $`.rulesync/hooks/format.sh`;
      }
    },
  };
};
